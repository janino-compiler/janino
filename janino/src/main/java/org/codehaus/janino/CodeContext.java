
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2010 Arno Unkrig. All rights reserved.
 * Copyright (c) 2015-2016 TIBCO Software Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *       following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *       following disclaimer in the documentation and/or other materials provided with the distribution.
 *    3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote
 *       products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.codehaus.janino;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.codehaus.commons.nullanalysis.Nullable;
import org.codehaus.janino.Java.LocalVariableSlot;
import org.codehaus.janino.util.ClassFile;
import org.codehaus.janino.util.ClassFile.AttributeInfo;
import org.codehaus.janino.util.ClassFile.LineNumberTableAttribute.Entry;

/**
 * The context of the compilation of a function (constructor or method). Manages generation of byte code, the exception
 * table, generation of line number tables, allocation of local variables, determining of stack size and local variable
 * table size and flow analysis.
 */
public
class CodeContext {

    private static final Logger LOGGER = Logger.getLogger(CodeContext.class.getName());

    private static final int     INITIAL_SIZE   = 128;
    private static final byte    UNEXAMINED     = -1;
    private static final byte    INVALID_OFFSET = -2;
    private static final int     MAX_STACK_SIZE = 65535;

    private final ClassFile classFile;
    private final String    functionName;

    private int                             maxStack;
    private short                           maxLocals;
    private byte[]                          code;
    private final Offset                    beginning;
    private final Inserter                  end;
    private Inserter                        currentInserter;
    private final List<ExceptionTableEntry> exceptionTableEntries;

    /**
     * All the local variables that are allocated in any block in this {@link CodeContext}.
     */
    private final List<Java.LocalVariableSlot> allLocalVars = new ArrayList<Java.LocalVariableSlot>();

    /**
     * Each List of Java.LocalVariableSlot is the local variables allocated for a block. They are pushed and popped
     * onto the list together to make allocation of the next local variable slot easy.
     */
    private LocalScope currentLocalScope = null;

    static class LocalScope {
        final LocalScope parent;
        final short startingLocalVariableSlot;
        final List<Java.LocalVariableSlot> localVars = new ArrayList<Java.LocalVariableSlot>();

        LocalScope(LocalScope parent, short startingLocalSlot) {
            this.parent = parent;
            this.startingLocalVariableSlot = startingLocalSlot;
        }
    }

    private short                   nextLocalVariableSlot;
    private final List<Relocatable> relocatables = new ArrayList<Relocatable>();

    /**
     * Creates an empty "Code" attribute.
     */
    public
    CodeContext(ClassFile classFile, String functionName) {
        this.classFile    = classFile;
        this.functionName = functionName;

        this.maxStack              = 0;
        this.maxLocals             = 0;
        this.code                  = new byte[CodeContext.INITIAL_SIZE];
        this.beginning             = new Offset();
        this.end                   = new Inserter();
        this.currentInserter       = this.end;
        this.exceptionTableEntries = new ArrayList<ExceptionTableEntry>();

        this.beginning.offset = 0;
        this.end.offset       = 0;
        this.beginning.next   = this.end;
        this.end.prev         = this.beginning;
    }

    /**
     * The {@link ClassFile} this context is related to.
     */
    public ClassFile
    getClassFile() { return this.classFile; }


    /**
     * Allocates space for a local variable of the given size (1 or 2) on the local variable array.
     * <p>
     *   As a side effect, the "max_locals" field of the "Code" attribute is updated.
     * </p>
     * <p>
     *   The only way to deallocate local variables is to {@link #saveLocalVariables()} and later {@link
     *   #restoreLocalVariables()}.
     * </p>
     *
     * @param size The number of slots to allocate (1 or 2)
     * @return The slot index of the allocated variable
     */
    public short
    allocateLocalVariable(short size) { return this.allocateLocalVariable(size, null, null).getSlotIndex(); }

    /**
     * Allocates space for a local variable of the given size (1 or 2) on the local variable array. As a side effect,
     * the "max_locals" field of the "Code" attribute is updated.
     * <p>
     *   The only way to deallocate local variables is to {@link #saveLocalVariables()} and later {@link
     *   #restoreLocalVariables()}.
     * </p>
     *
     * @param size Number of slots to use (1 or 2)
     * @param name The variable name; if {@code null}, then the variable won't be written to the LocalVariableTable
     * @param type The variable type; if the <var>name</var> is not null, then the type is needed to write to the
     *             LocalVariableTable
     */
    public Java.LocalVariableSlot
    allocateLocalVariable(short size, @Nullable String name, @Nullable IClass type) {
        List<Java.LocalVariableSlot> currentVars = null;

        if (this.currentLocalScope == null) {
            throw new Error("saveLocalVariables must be called first");
        } else {
            LocalScope currentScope = this.currentLocalScope;
            currentVars = currentScope.localVars;
        }

        Java.LocalVariableSlot slot = new Java.LocalVariableSlot(name, this.nextLocalVariableSlot, type);

        if (name != null) {
            slot.setStart(this.newOffset());
        }

        this.nextLocalVariableSlot += size;
        currentVars.add(slot);
        this.allLocalVars.add(slot);

        if (this.nextLocalVariableSlot > this.maxLocals) {
            this.maxLocals = this.nextLocalVariableSlot;
        }

        return slot;
    }

    /**
     * Remembers the current size of the local variables array.
     */
    public List<Java.LocalVariableSlot>
    saveLocalVariables() {

        // Push empty list on the stack to hold a new block's local vars.
        LocalScope newScope = new LocalScope(this.currentLocalScope, this.nextLocalVariableSlot);
        this.currentLocalScope = newScope;

        return newScope.localVars;
    }

    /**
     * Restores the previous size of the local variables array. This MUST to be called for every call to
     * saveLocalVariables as it closes the variable extent for all the active local variables in the current block.
     */
    public void
    restoreLocalVariables() {

        // Pop the list containing the current block's local vars.
        LocalScope scopeToPop = this.currentLocalScope;
        this.currentLocalScope = scopeToPop.parent;
        List<Java.LocalVariableSlot> slots = scopeToPop.localVars;

        for (Java.LocalVariableSlot slot : slots) {
            if (slot.getName() != null) {
                slot.setEnd(this.newOffset());
            }
        }

        // reuse local variable slots of the popped scope
        this.nextLocalVariableSlot = scopeToPop.startingLocalVariableSlot;
    }

    /**
     * @param lineNumberTableAttributeNameIndex 0 == don't generate a "LineNumberTable" attribute
     */
    protected void
    storeCodeAttributeBody(
        DataOutputStream dos,
        short            lineNumberTableAttributeNameIndex,
        short            localVariableTableAttributeNameIndex,
        short            stackMapTableAttributeNameIndex
    ) throws IOException {
        dos.writeShort(this.maxStack);                                               // max_stack
        dos.writeShort(this.maxLocals);                                              // max_locals
        dos.writeInt(this.end.offset);                                               // code_length
        dos.write(this.code, 0, this.end.offset);                                    // code
        dos.writeShort(this.exceptionTableEntries.size());                           // exception_table_length
        for (ExceptionTableEntry exceptionTableEntry : this.exceptionTableEntries) { // exception_table
            dos.writeShort(exceptionTableEntry.startPC.offset);
            dos.writeShort(exceptionTableEntry.endPC.offset);
            dos.writeShort(exceptionTableEntry.handlerPC.offset);
            dos.writeShort(exceptionTableEntry.catchType);
        }

        List<ClassFile.AttributeInfo> attributes = new ArrayList<AttributeInfo>();

        // Add "LineNumberTable" attribute.
        if (lineNumberTableAttributeNameIndex != 0) {
            List<ClassFile.LineNumberTableAttribute.Entry> lnt = new ArrayList<Entry>();
            for (Offset o = this.beginning; o != null; o = o.next) {
                if (o instanceof LineNumberOffset) {

                    int offset = o.offset;
                    if (offset > 0xffff) {
                        throw new InternalCompilerException("LineNumberTable entry offset out of range");
                    }

                    short lineNumber = ((LineNumberOffset) o).lineNumber;

                    lnt.add(new ClassFile.LineNumberTableAttribute.Entry((short) offset, lineNumber));
                }
            }
            ClassFile.LineNumberTableAttribute.Entry[] lnte = (ClassFile.LineNumberTableAttribute.Entry[]) lnt.toArray(
                new ClassFile.LineNumberTableAttribute.Entry[lnt.size()]
            );
            attributes.add(new ClassFile.LineNumberTableAttribute(
                lineNumberTableAttributeNameIndex, // attributeNameIndex
                lnte                               // lineNumberTableEntries
            ));
        }

        // Add "LocalVariableTable" attribute.
        if (localVariableTableAttributeNameIndex != 0) {
            ClassFile.AttributeInfo ai = this.storeLocalVariableTable(dos, localVariableTableAttributeNameIndex);

            if (ai != null) attributes.add(ai);
        }

//        // Add "StackMapTable" attribute.
//        attributes.add(new ClassFile.AttributeInfo(stackMapTableAttributeNameIndex) {
//            @Override protected void
//            storeBody(DataOutputStream dos) throws IOException {
//                if (CodeContext.this.functionName.equals("eval0()Ljava/lang/Object;")) {
//                    short occi = CodeContext.this.getClassFile().addConstantClassInfo("Ljava/lang/Object;");
//                    short scci = CodeContext.this.getClassFile().addConstantClassInfo("Ljava/lang/String;");
//                    dos.write(new byte[] {
//
//                        // xxx1:
//                        0, 3,    // number_of_entries
//                        66,      // entries[0]: 3: same_locals_1_stack_item_frame(stack=String)
//                            7, (byte) (scci >> 8), (byte) scci,
//                        78,      // entries[1]: 17: same_locals_1_stack_item_frame(stack=String)
//                            7, (byte) (scci >> 8), (byte) scci,
//                        -1,      // entries[2]: 19: full_frame(offsetDelta=1, locals=[], stack=[String, Object])
//                            0, 1,                               // offset_delta
//                            0, 0,                               // number_of_locals
//                            0, 2,                               // number_of_stack_items
//                            7, (byte) (scci >> 8), (byte) scci, // stack[0]: String
//                            7, (byte) (occi >> 8), (byte) occi, // stack[1]: Object
//
////                        // xxx2:
////                        0, 2,    // number_of_entries
////                        15,      // entries[0]: 15: same_frame
////                        64, 1,   // entries[1]: 16: same_locals_1_stack_item_frame(stack=int)
//                    });
//                } else {
//                    dos.writeShort(0);
//                }
//            }
//        });

        dos.writeShort(attributes.size());                     // attributes_count
        for (ClassFile.AttributeInfo attribute : attributes) { // attributes;
            attribute.store(dos);
        }
    }

    /**
     * @return A {@link org.codehaus.janino.util.ClassFile.LocalVariableTableAttribute} for this {@link CodeContext}
     */
    @Nullable protected ClassFile.AttributeInfo
    storeLocalVariableTable(DataOutputStream dos, short localVariableTableAttributeNameIndex) {

        ClassFile cf = this.getClassFile();

        final List<ClassFile.LocalVariableTableAttribute.Entry>
        entryList = new ArrayList<org.codehaus.janino.util.ClassFile.LocalVariableTableAttribute.Entry>();

        for (Java.LocalVariableSlot slot : this.getAllLocalVars()) {

            String localVariableName = slot.getName();
            if (localVariableName != null) {

                String      typeName    = slot.getType().getDescriptor();
                final short classSlot   = cf.addConstantUtf8Info(typeName);
                final short varNameSlot = cf.addConstantUtf8Info(localVariableName);

                Offset start = slot.getStart();
                Offset end2  = slot.getEnd();

                assert start != null;
                assert end2 != null;

                ClassFile.LocalVariableTableAttribute.Entry entry = new ClassFile.LocalVariableTableAttribute.Entry(
                    (short) start.offset,
                    (short) (end2.offset - start.offset),
                    varNameSlot,
                    classSlot,
                    slot.getSlotIndex()
                );
                entryList.add(entry);
            }
        }

        if (entryList.size() > 0) {
            Object entries = entryList.toArray(new ClassFile.LocalVariableTableAttribute.Entry[entryList.size()]);

            return new ClassFile.LocalVariableTableAttribute(
                localVariableTableAttributeNameIndex,
                (ClassFile.LocalVariableTableAttribute.Entry[]) entries
            );
        }

        return null;
    }

    /**
     * Checks the code for consistency; updates the "maxStack" member.
     *
     * @throws InternalCompilerException The bytecode is inconsistent wrt/ the operand stack
     */
    public void
    flowAnalysis(String functionName) {
        CodeContext.LOGGER.entering(null, "flowAnalysis", functionName);

        int[] stackSizes = new int[this.end.offset];
        Arrays.fill(stackSizes, CodeContext.UNEXAMINED);

        // Analyze flow from offset zero.
        this.flowAnalysis(
            functionName,
            this.code,       // code
            this.end.offset, // codeSize
            0,               // offset
            0,               // stackSize
            stackSizes       // stackSizes
        );

        // Analyze flow from exception handler entry points.
        int analyzedExceptionHandlers = 0;
        while (analyzedExceptionHandlers != this.exceptionTableEntries.size()) {
            for (ExceptionTableEntry exceptionTableEntry : this.exceptionTableEntries) {
                if (stackSizes[exceptionTableEntry.startPC.offset] != CodeContext.UNEXAMINED) {
                    this.flowAnalysis(
                        functionName,
                        this.code,                                          // code
                        this.end.offset,                                    // codeSize
                        exceptionTableEntry.handlerPC.offset,               // offset
                        stackSizes[exceptionTableEntry.startPC.offset] + 1, // stackSize
                        stackSizes                                          // stackSizes
                    );
                    ++analyzedExceptionHandlers;
                }
            }
        }

        // Check results and determine maximum stack size.
        this.maxStack = 0;
        for (int i = 0; i < stackSizes.length; ++i) {
            int ss = stackSizes[i];

            if (ss == CodeContext.UNEXAMINED) {
                String message = functionName + ": Unexamined code at offset " + i;
                if (CodeContext.LOGGER.isLoggable(Level.FINE)) {
                    CodeContext.LOGGER.fine(message);

                    // Complete normally, so the .class file is created and can be examined.
                    return;
                }
                throw new InternalCompilerException(message);
            }

            if (ss > this.maxStack) this.maxStack = ss;
        }
    }

    /**
     * @param functionName
     * @param code
     * @param codeSize
     * @param offset       Where to start the analysis
     * @param stackSize    Stack size on start
     * @param stackSizes   Stack sizes at offsets within <var>code</var>; {@link #UNEXAMINED} value
     *                     indicates that the stack size at a given offset has not yet been
     *                     calculated
     */
    private void
    flowAnalysis(
        String functionName,
        byte[] code,
        int    codeSize,
        int    offset,
        int    stackSize,
        int[]  stackSizes
    ) {
        for (;;) {
            CodeContext.LOGGER.entering(
                null,
                "flowAnalysis",
                new Object[] { functionName, code, codeSize, offset, stackSize, stackSizes }
            );

            // Check current bytecode offset.
            if (offset < 0 || offset >= codeSize) {
                throw new InternalCompilerException(functionName + ": Offset " + offset + " is out of range");
            }

            // Have we hit an area that has already been analyzed?
            int css = stackSizes[offset];
            if (css == stackSize) return; // OK.
            if (css == CodeContext.INVALID_OFFSET) {
                String message = functionName + ": Invalid offset";
                if (CodeContext.LOGGER.isLoggable(Level.FINE)) {
                    CodeContext.LOGGER.fine(message);

                    // Complete normally, so the .class file is created and can be examined.
                    return;
                }
                throw new InternalCompilerException(message);
            }
            if (css != CodeContext.UNEXAMINED) {
                String message = (
                    functionName
                    + ": Operand stack inconsistent at offset "
                    + offset
                    + ": Previous size "
                    + css
                    + ", now "
                    + stackSize
                );
                if (CodeContext.LOGGER.isLoggable(Level.FINE)) {
                    CodeContext.LOGGER.fine(message);

                    // Complete normally, so the .class file is created and can be examined.
                    return;
                }
                throw new InternalCompilerException(message);
            }
            stackSizes[offset] = stackSize;

            // Analyze current opcode.
            byte  opcode        = code[offset];
            int   operandOffset = offset + 1;
            short props;
            if (opcode == Opcode.WIDE) {
                opcode = code[operandOffset++];
                props  = Opcode.WIDE_OPCODE_PROPERTIES[0xff & opcode];
            } else {
                props = Opcode.OPCODE_PROPERTIES[0xff & opcode];
            }
            if (props == Opcode.INVALID_OPCODE) {
                throw new InternalCompilerException(
                    functionName
                    + ": Invalid opcode "
                    + (0xff & opcode)
                    + " at offset "
                    + offset
                );
            }

            switch (props & Opcode.SD_MASK) {

            case Opcode.SD_M4:
            case Opcode.SD_M3:
            case Opcode.SD_M2:
            case Opcode.SD_M1:
            case Opcode.SD_P0:
            case Opcode.SD_P1:
            case Opcode.SD_P2:
                stackSize += (props & Opcode.SD_MASK) - Opcode.SD_P0;
                break;

            case Opcode.SD_0:
                stackSize = 0;
                break;

            case Opcode.SD_GETFIELD:
                --stackSize;
            case Opcode.SD_GETSTATIC: // SUPPRESS CHECKSTYLE FallThrough
                stackSize += this.determineFieldSize(CodeContext.extract16BitValue(operandOffset, code));
                break;

            case Opcode.SD_PUTFIELD:
                --stackSize;
            case Opcode.SD_PUTSTATIC: // SUPPRESS CHECKSTYLE FallThrough
                stackSize -= this.determineFieldSize(CodeContext.extract16BitValue(operandOffset, code));
                break;

            case Opcode.SD_INVOKEVIRTUAL:
            case Opcode.SD_INVOKESPECIAL:
            case Opcode.SD_INVOKEINTERFACE:
                --stackSize;
            case Opcode.SD_INVOKESTATIC: // SUPPRESS CHECKSTYLE FallThrough
                stackSize -= this.determineArgumentsSize(CodeContext.extract16BitValue(operandOffset, code));
                break;

            case Opcode.SD_MULTIANEWARRAY:
                stackSize -= code[operandOffset + 2] - 1;
                break;

            default:
                throw new InternalCompilerException(functionName + ": Invalid stack delta");
            }

            if (stackSize < 0) {
                String message = (
                    this.classFile.getThisClassName()
                    + '.'
                    + functionName
                    + ": Operand stack underrun at offset "
                    + offset
                );
                if (CodeContext.LOGGER.isLoggable(Level.FINE)) {
                    CodeContext.LOGGER.fine(message);

                    // Complete normally, so the .class file is created and can be examined.
                    return;
                }
                throw new InternalCompilerException(message);
            }

            if (stackSize > CodeContext.MAX_STACK_SIZE) {
                String message = (
                    this.classFile.getThisClassName()
                    + '.'
                    + functionName
                    + ": Operand stack overflow at offset "
                    + offset
                );
                if (CodeContext.LOGGER.isLoggable(Level.FINE)) {
                    CodeContext.LOGGER.fine(message);

                    // Complete normally, so the .class file is created and can be examined.
                    return;
                }
                throw new InternalCompilerException(message);
            }

            switch (props & Opcode.OP1_MASK) {

            case 0:
                ;
                break;

            case Opcode.OP1_SB:
            case Opcode.OP1_UB:
            case Opcode.OP1_CP1:
            case Opcode.OP1_LV1:
                ++operandOffset;
                break;

            case Opcode.OP1_SS:
            case Opcode.OP1_CP2:
            case Opcode.OP1_LV2:
                operandOffset += 2;
                break;

            case Opcode.OP1_BO2:
                this.flowAnalysis(
                    functionName,
                    code,
                    codeSize,
                    CodeContext.extract16BitValue(offset, operandOffset, code),
                    stackSize,
                    stackSizes
                );
                operandOffset += 2;
                break;

            case Opcode.OP1_JSR:
                int targetOffset = CodeContext.extract16BitValue(offset, operandOffset, code);
                operandOffset += 2;
                if (stackSizes[targetOffset] == CodeContext.UNEXAMINED) {
                    this.flowAnalysis(
                        functionName,
                        code,
                        codeSize,
                        targetOffset,
                        stackSize + 1,
                        stackSizes
                    );
                }
                break;

            case Opcode.OP1_BO4:
                this.flowAnalysis(
                    functionName,
                    code,
                    codeSize,
                    CodeContext.extract32BitValue(offset, operandOffset, code),
                    stackSize,
                    stackSizes
                );
                operandOffset += 4;
                break;

            case Opcode.OP1_LOOKUPSWITCH:
                while ((operandOffset & 3) != 0) ++operandOffset;
                this.flowAnalysis(
                    functionName,
                    code,
                    codeSize,
                    CodeContext.extract32BitValue(offset, operandOffset, code),
                    stackSize,
                    stackSizes
                );
                operandOffset += 4;

                int npairs = CodeContext.extract32BitValue(0, operandOffset, code);
                operandOffset += 4;

                for (int i = 0; i < npairs; ++i) {
                    operandOffset += 4; //skip match value
                    this.flowAnalysis(
                        functionName,
                        code,
                        codeSize,
                        CodeContext.extract32BitValue(offset, operandOffset, code),
                        stackSize,
                        stackSizes
                    );
                    operandOffset += 4; //advance over offset
                }
                break;

            case Opcode.OP1_TABLESWITCH:
                while ((operandOffset & 3) != 0) ++operandOffset;
                this.flowAnalysis(
                    functionName,
                    code,
                    codeSize,
                    CodeContext.extract32BitValue(offset, operandOffset, code),
                    stackSize,
                    stackSizes
                );
                operandOffset += 4;
                int low = CodeContext.extract32BitValue(offset, operandOffset, code);
                operandOffset += 4;
                int hi = CodeContext.extract32BitValue(offset, operandOffset, code);
                operandOffset += 4;
                for (int i = low; i <= hi; ++i) {
                    this.flowAnalysis(
                        functionName,
                        code,
                        codeSize,
                        CodeContext.extract32BitValue(offset, operandOffset, code),
                        stackSize,
                        stackSizes
                    );
                    operandOffset += 4;
                }
                break;

            default:
                throw new InternalCompilerException(functionName + ": Invalid OP1");
            }

            switch (props & Opcode.OP2_MASK) {

            case 0:
                ;
                break;

            case Opcode.OP2_SB:
                ++operandOffset;
                break;

            case Opcode.OP2_SS:
                operandOffset += 2;
                break;

            default:
                throw new InternalCompilerException(functionName + ": Invalid OP2");
            }

            switch (props & Opcode.OP3_MASK) {

            case 0:
                ;
                break;

            case Opcode.OP3_SB:
                ++operandOffset;
                break;

            default:
                throw new InternalCompilerException(functionName + ": Invalid OP3");
            }

            Arrays.fill(stackSizes, offset + 1, operandOffset, CodeContext.INVALID_OFFSET);

            if ((props & Opcode.NO_FALLTHROUGH) != 0) return;
            offset = operandOffset;
        }
    }

    /**
     * Extracts a 16 bit value at the given <var>offset</var> in the <var>code</var>.
     */
    private static short
    extract16BitValue(int offset, byte[] code) {
        CodeContext.LOGGER.entering(
            null,
            "extract16BitValue",
            new Object[] { offset, code[offset], code[offset + 1] }
        );

        short result = (short) (((code[offset]) << 8) + (code[offset + 1] & 0xff));

        CodeContext.LOGGER.exiting(null,  "extract16BitValue", result);
        return result;
    }

    /**
     * Extracts a 16 bit value at the given <var>offset</var> in the <var>code</var> and adds a
     * <var>bias</var> to it.
     *
     * @return An integer that treats the two bytes at position offset as an UNSIGNED SHORT
     */
    private static int
    extract16BitValue(int bias, int offset, byte[] code) {
        CodeContext.LOGGER.entering(
            null,
            "extract16BitValue",
            new Object[] { bias, offset, code[offset], code[offset + 1] }
        );

        int result = bias + ((code[offset]) << 8) + (code[offset + 1] & 0xff);
        CodeContext.LOGGER.exiting(null,  "extract16BitValue", result);
        return result;
    }

    /**
     * Extracts a 32 bit value at offset in code and adds <var>bias</var> to it.
     *
     * @param bias   An int to skew the final result by (useful for calculating relative offsets)
     * @param offset The position in the code array to extract the bytes from
     * @param code   The array of bytes
     * @return       The 4 bytes at position offset + bias
     */
    private static int
    extract32BitValue(int bias, int offset, byte[] code) {
        CodeContext.LOGGER.entering(
            null,
            "extract32BitValue",
            new Object[] { bias, offset, code[offset], code[offset + 1], code[offset + 2], code[offset + 3] }
        );

        int result = bias + (
            (code[offset] << 24)
            + ((0xff & code[offset + 1]) << 16)
            + ((0xff & code[offset + 2]) << 8)
            + (0xff & code[offset + 3])
        );

        CodeContext.LOGGER.exiting(null, "extract32BitValue", result);
        return result;
    }

    /**
     * Fixes up all of the offsets and relocate() all relocatables.
     */
    public void
    fixUpAndRelocate() {

        // We do this in a loop to allow relocatables to adjust the size
        // of things in the byte stream.  It is extremely unlikely, but possible
        // that a late relocatable will grow the size of the bytecode, and require
        // an earlier relocatable to switch from 32K mode to 64K mode branching
        do {
            this.fixUp();
        } while (!this.relocate());
    }

    /**
     * Fixes up all offsets.
     */
    private void
    fixUp() {
        for (Offset o = this.beginning; o != this.end; o = o.next) {
            assert o != null;
            if (o instanceof FixUp) ((FixUp) o).fixUp();
        }
    }

    /**
     * Relocates all relocatables and aggregate their response into a single one.
     *
     * @return {@code true} if all of them relocated successfully, {@code false} if any of them needed to change size
     */
    private boolean
    relocate() {
        boolean finished = true;
        for (Relocatable relocatable : this.relocatables) {

            // Do not terminate earlier so that everything gets a chance to grow in the first pass changes the common
            // case for this to be O(n) instead of O(n**2).
            finished &= relocatable.relocate();
        }
        return finished;
    }

    /**
     * Analyzes the descriptor of the Fieldref at index <var>idx</var> and return its size.
     *
     * @see Descriptor#size(String)
     */
    private int
    determineFieldSize(short idx) {
        return Descriptor.size(
            this
            .classFile
            .getConstantFieldrefInfo(idx)
            .getNameAndType(this.classFile)
            .getDescriptor(this.classFile)
        );
    }

    /**
     * Analyzes the descriptor of the Methodref and returns the sum of the arguments' sizes minus the return value's
     * size.
     */
    private int
    determineArgumentsSize(short idx) {
        ClassFile.ConstantPoolInfo        cpi = this.classFile.getConstantPoolInfo(idx);
        ClassFile.ConstantNameAndTypeInfo nat = (
            cpi instanceof ClassFile.ConstantInterfaceMethodrefInfo
            ? ((ClassFile.ConstantInterfaceMethodrefInfo) cpi).getNameAndType(this.classFile)
            : ((ClassFile.ConstantMethodrefInfo)          cpi).getNameAndType(this.classFile)
        );
        String desc = nat.getDescriptor(this.classFile);

        if (desc.charAt(0) != '(') throw new InternalCompilerException("Method descriptor does not start with \"(\"");
        int i   = 1;
        int res = 0;
        for (;;) {
            switch (desc.charAt(i++)) {
            case ')':
                return res - Descriptor.size(desc.substring(i));
            case 'B': case 'C': case 'F': case 'I': case 'S': case 'Z':
                res += 1;
                break;
            case 'D': case 'J':
                res += 2;
                break;
            case '[':
                res += 1;
                while (desc.charAt(i) == '[') ++i;
                if ("BCFISZDJ".indexOf(desc.charAt(i)) != -1) { ++i; break; }
                if (desc.charAt(i) != 'L') throw new InternalCompilerException("Invalid char after \"[\"");
                ++i;
                while (desc.charAt(i++) != ';');
                break;
            case 'L':
                res += 1;
                while (desc.charAt(i++) != ';');
                break;
            default:
                throw new InternalCompilerException("Invalid method descriptor");
            }
        }
    }

    /**
     * Inserts a sequence of bytes at the current insertion position. Creates {@link LineNumberOffset}s as necessary.
     *
     * @param lineNumber The line number that corresponds to the byte code, or -1
     * @param b
     */
    public void
    write(int lineNumber, byte[] b) {

        if (b.length == 0) return;

        // CAVEAT: "makeSpace()" may change "this.code", so make sure to call "makeSpace()" _before_ using "this.code"!
        int o = this.makeSpace(lineNumber, b.length);
        System.arraycopy(b, 0, this.code, o, b.length);
    }

    /**
     * Inserts a byte at the current insertion position. Creates {@link LineNumberOffset}s as necessary.
     * <p>
     *   This method is an optimization to avoid allocating small byte[] and ease GC load.
     * </p>
     *
     * @param lineNumber The line number that corresponds to the byte code, or -1
     * @param b1
     */
    public void
    write(int lineNumber, byte b1) {

        // CAVEAT: "makeSpace()" may change "this.code", so make sure to call "makeSpace()" _before_ using "this.code"!
        int o = this.makeSpace(lineNumber, 1);
        this.code[o] = b1;
    }

    /**
     * Inserts bytes at the current insertion position. Creates {@link LineNumberOffset}s as necessary.
     * <p>
     *   This method is an optimization to avoid allocating small byte[] and ease GC load.
     * </p>
     *
     * @param lineNumber The line number that corresponds to the byte code, or -1
     */
    public void
    write(int lineNumber, byte b1, byte b2) {

        int o = this.makeSpace(lineNumber, 2);

        this.code[o++] = b1;
        this.code[o]   = b2;
    }

    /**
     * Inserts bytes at the current insertion position. Creates {@link LineNumberOffset}s as necessary.
     * <p>
     *   This method is an optimization to avoid allocating small byte[] and ease GC load.
     * </p>
     *
     * @param lineNumber The line number that corresponds to the byte code, or -1
     */
    public void
    write(int lineNumber, byte b1, byte b2, byte b3) {

        int o = this.makeSpace(lineNumber, 3);

        this.code[o++] = b1;
        this.code[o++] = b2;
        this.code[o]   = b3;
    }

    /**
     * Inserts bytes at the current insertion position. Creates {@link LineNumberOffset}s as necessary.
     * <p>
     *   This method is an optimization to avoid allocating small byte[] and ease GC load.
     * </p>
     *
     * @param lineNumber The line number that corresponds to the byte code, or -1
     */
    public void
    write(int lineNumber, byte b1, byte b2, byte b3, byte b4) {

        int o = this.makeSpace(lineNumber, 4);

        this.code[o++] = b1;
        this.code[o++] = b2;
        this.code[o++] = b3;
        this.code[o]   = b4;
    }

    /**
     * Inserts <var>size</var> NUL bytes at the current inserter's offset, advances the current inserter's offset by
     * <var>size</var>, creates {@link LineNumberOffset}s as necessary, and returns the current inserter's original
     * offset (the offset of the first NUL byte that was inserted).
     * <p>
     *   Because the class file format does not support line numbers greater than 65535, these are treated as 65535.
     * </p>
     *
     * @param lineNumber -1 indicates that no particular line in the source code corresponds to this offset
     * @param size       The number of NUL bytes to inject
     * @return           The offset of the first inserted byte
     */
    public int
    makeSpace(int lineNumber, final int size) {

        final int cio = this.currentInserter.offset;

        if (size == 0) return cio;

        INSERT_LINE_NUMBER_OFFSET:
        if (lineNumber != -1) {

            if (lineNumber > 0xffff) lineNumber = 0xffff;

            // Find out whether the line number is different from the line number of the preceding insertion,
            // and, if so, insert a LineNumberOffset object, which will later lead to a LineNumberTable entry.
            for (Offset o = this.currentInserter.prev; o != this.beginning; o = o.prev) {
                assert o != null;
                if (o instanceof LineNumberOffset) {
                    if ((((LineNumberOffset) o).lineNumber & 0xffff) == lineNumber) {
                        break INSERT_LINE_NUMBER_OFFSET;
                    }
                    break;
                }
            }

            // Insert a LineNumberOffset _before_ the current inserter.
            LineNumberOffset lno = new LineNumberOffset(cio, (short) lineNumber);

            Offset cip = this.currentInserter.prev;
            assert cip != null;

            lno.prev = cip;
            lno.next = this.currentInserter;

            cip.next = lno;

            this.currentInserter.prev = lno;
        }

        if (this.end.offset + size <= this.code.length) {

            // Optimization to avoid a trivial method call in the common case
            if (cio != this.end.offset) {
                System.arraycopy(this.code, cio, this.code, cio + size, this.end.offset - cio);
            }
        } else {
            byte[] oldCode = this.code;
            //double size to avoid horrible performance, but don't grow over our limit
            int newSize = Math.max(Math.min(oldCode.length * 2, 0xffff), oldCode.length + size);
            if (newSize > 0xffff) {
                throw new InternalCompilerException(
                    "Code of method \""
                    + this.functionName
                    + "\" of class \""
                    + this.classFile.getThisClassName()
                    + "\" grows beyond 64 KB"
                );
            }
            this.code = new byte[newSize];
            System.arraycopy(oldCode, 0, this.code, 0, cio);
            System.arraycopy(oldCode, cio, this.code, cio + size, this.end.offset - cio);
        }
        Arrays.fill(this.code, cio, cio + size, (byte) 0);
        for (Offset o = this.currentInserter; o != null; o = o.next) o.offset += size;

        return cio;
    }

    /**
     */
    public void
    writeShort(int v) { this.write(-1, (byte) (v >> 8), (byte) v); }

    /**
     * Generates a "branch" instruction.
     *
     * @param lineNumber The line number that corresponds to the byte code, or -1
     * @param opcode     One of {@link Opcode#GOTO}, {@link Opcode#JSR} and <code>Opcode.IF*</code>
     * @param dst        Where to branch
     */
    public void
    writeBranch(int lineNumber, int opcode, final Offset dst) {
        this.relocatables.add(new Branch(opcode, dst));
        this.write(lineNumber, (byte) opcode, (byte) -1, (byte) -1);
    }

    private
    class Branch extends Relocatable {

        Branch(int opcode, Offset destination) {
            this.opcode      = opcode;
            this.source      = CodeContext.this.newInserter();
            this.destination = destination;
            if (opcode == Opcode.JSR_W || opcode == Opcode.GOTO_W) {

                // No need to expand wide opcodes.
                this.expanded = true;
            } else {
                this.expanded = false;
            }
        }

        @Override public boolean
        relocate() {
            if (this.destination.offset == Offset.UNSET) {
                throw new InternalCompilerException("Cannot relocate branch to unset destination offset");
            }
            int offset = this.destination.offset - this.source.offset;

            if (!this.expanded && (offset > Short.MAX_VALUE || offset < Short.MIN_VALUE)) {
                //we want to insert the data without skewing our source position,
                //so we will cache it and then restore it later.
                final int pos = this.source.offset;
                CodeContext.this.pushInserter(this.source);
                {
                    // Promotion to a wide instruction only requires 2 extra bytes. Everything else requires a new
                    // GOTO_W instruction after a negated if (5 extra bytes).
                    CodeContext.this.makeSpace(
                        -1,
                        this.opcode == Opcode.GOTO || this.opcode == Opcode.JSR ? 2 : 5
                    );
                }
                CodeContext.this.popInserter();
                this.source.offset = pos;
                this.expanded      = true;
                return false;
            }

            final byte[] ba;
            if (!this.expanded) {
                //we fit in a 16-bit jump
                ba = new byte[] { (byte) this.opcode, (byte) (offset >> 8), (byte) offset };
            } else {
                if (this.opcode == Opcode.GOTO || this.opcode == Opcode.JSR) {
                    ba = new byte[] {
                        (byte) (this.opcode + 33), // GOTO => GOTO_W; JSR => JSR_W
                        (byte) (offset >> 24),
                        (byte) (offset >> 16),
                        (byte) (offset >> 8),
                        (byte) offset
                    };
                } else
                {
                    //exclude the if-statement from jump target
                    //if jumping backwards this will increase the jump to go over it
                    //if jumping forwards this will decrease the jump by it
                    offset -= 3;

                    //  [if cond offset]
                    //expands to
                    //  [if !cond skip_goto]
                    //  [GOTO_W offset]
                    ba = new byte[] {
                        CodeContext.invertBranchOpcode((byte) this.opcode),
                        0,
                        8, // Jump from this instruction past the GOTO_W
                        Opcode.GOTO_W,
                        (byte) (offset >> 24),
                        (byte) (offset >> 16),
                        (byte) (offset >> 8),
                        (byte) offset
                    };
                }
            }
            System.arraycopy(ba, 0, CodeContext.this.code, this.source.offset, ba.length);
            return true;
        }

        private boolean        expanded; //marks whether this has been expanded to account for a wide branch
        private final int      opcode;
        private final Inserter source;
        private final Offset   destination;
    }

    /**
     * E.g. {@link Opcode#IFLT} ("less than") inverts to {@link Opcode#IFGE} ("greater than or equal to").
     */
    private static byte
    invertBranchOpcode(byte branchOpcode) {
        return (Byte) CodeContext.BRANCH_OPCODE_INVERSION.get(new Byte(branchOpcode));
    }

    private static final Map<Byte /*branch-opcode*/, Byte /*inverted-branch-opcode*/>
    BRANCH_OPCODE_INVERSION = CodeContext.createBranchOpcodeInversion();
    private static Map<Byte, Byte>
    createBranchOpcodeInversion() {
        Map<Byte, Byte> m = new HashMap<Byte, Byte>();
        m.put(new Byte(Opcode.IF_ACMPEQ), new Byte(Opcode.IF_ACMPNE));
        m.put(new Byte(Opcode.IF_ACMPNE), new Byte(Opcode.IF_ACMPEQ));
        m.put(new Byte(Opcode.IF_ICMPEQ), new Byte(Opcode.IF_ICMPNE));
        m.put(new Byte(Opcode.IF_ICMPNE), new Byte(Opcode.IF_ICMPEQ));
        m.put(new Byte(Opcode.IF_ICMPGE), new Byte(Opcode.IF_ICMPLT));
        m.put(new Byte(Opcode.IF_ICMPLT), new Byte(Opcode.IF_ICMPGE));
        m.put(new Byte(Opcode.IF_ICMPGT), new Byte(Opcode.IF_ICMPLE));
        m.put(new Byte(Opcode.IF_ICMPLE), new Byte(Opcode.IF_ICMPGT));
        m.put(new Byte(Opcode.IFEQ),      new Byte(Opcode.IFNE));
        m.put(new Byte(Opcode.IFNE),      new Byte(Opcode.IFEQ));
        m.put(new Byte(Opcode.IFGE),      new Byte(Opcode.IFLT));
        m.put(new Byte(Opcode.IFLT),      new Byte(Opcode.IFGE));
        m.put(new Byte(Opcode.IFGT),      new Byte(Opcode.IFLE));
        m.put(new Byte(Opcode.IFLE),      new Byte(Opcode.IFGT));
        m.put(new Byte(Opcode.IFNULL),    new Byte(Opcode.IFNONNULL));
        m.put(new Byte(Opcode.IFNONNULL), new Byte(Opcode.IFNULL));
        return Collections.unmodifiableMap(m);
    }

    /**
     * Writes a four-byte offset (as it is used in TABLESWITCH and LOOKUPSWITCH) into this code context.
     */
    public void
    writeOffset(int lineNumber, Offset src, final Offset dst) {
        this.relocatables.add(new OffsetBranch(this.newOffset(), src, dst));
        this.makeSpace(lineNumber, 4);
    }

    private
    class OffsetBranch extends Relocatable {

        OffsetBranch(Offset where, Offset source, Offset destination) {
            this.where       = where;
            this.source      = source;
            this.destination = destination;
        }

        @Override public boolean
        relocate() {
            if (this.source.offset == Offset.UNSET || this.destination.offset == Offset.UNSET) {
                throw new InternalCompilerException("Cannot relocate offset branch to unset destination offset");
            }
            int    offset = this.destination.offset - this.source.offset;
            byte[] ba     = new byte[] {
                (byte) (offset >> 24),
                (byte) (offset >> 16),
                (byte) (offset >> 8),
                (byte) offset
            };
            System.arraycopy(ba, 0, CodeContext.this.code, this.where.offset, 4);
            return true;
        }
        private final Offset where, source, destination;
    }

    /**
     * Creates and inserts an {@link CodeContext.Offset} at the current inserter's current position.
     */
    public Offset
    newOffset() {
        Offset o = new Offset();
        o.set();
        return o;
    }

    /**
     * Allocates an {@link Inserter}, set it to the current offset, and inserts it before the current offset.
     * <p>
     *   In clear text, this means that you can continue writing to the "Code" attribute, then {@link
     *   #pushInserter(CodeContext.Inserter)} the {@link Inserter}, then write again (which inserts bytes into the
     *   "Code" attribute at the previously remembered position), and then {@link #popInserter()}.
     * </p>
     */
    public Inserter
    newInserter() { Inserter i = new Inserter(); i.set(); return i; }

    /**
     * @return The current inserter
     */
    public Inserter
    currentInserter() { return this.currentInserter; }

    /**
     * Remembers the current {@link Inserter}, then replaces it with the new one.
     */
    public void
    pushInserter(Inserter ins) {
        if (ins.nextInserter != null) {
            throw new InternalCompilerException("An Inserter can only be pushed once at a time");
        }
        ins.nextInserter     = this.currentInserter;
        this.currentInserter = ins;
    }

    /**
     * Replaces the current {@link Inserter} with the remembered one (see {@link #pushInserter(CodeContext.Inserter)}).
     */
    public void
    popInserter() {
        Inserter ni = this.currentInserter.nextInserter;
        if (ni == null) throw new InternalCompilerException("Code inserter stack underflow");
        this.currentInserter.nextInserter = null; // Mark it as "unpushed".
        this.currentInserter              = ni;
    }

    /**
     * A class that represents an offset within a "Code" attribute.
     * <p>
     *   The concept of an "offset" is that if one writes into the middle of a "Code" attribute, all offsets behind the
     *   insertion point are automatically shifted.
     * </p>
     */
    public
    class Offset {

        /**
         * The offset in the code attribute that this object represents.
         */
        int offset = Offset.UNSET;

        /**
         * Links to preceding and succeeding offsets. Both are {@code null} <em>before</em> {@link #set()} is called,
         * and both are non-{@code null} <em>after</em> {@link #set()} has been called. This implies that {@link
         * #set()} must be invoked at most <em>once</em>.
         */
        @Nullable Offset prev, next;

        /**
         * Special value for {@link #offset} which indicates that this {@link Offset} has not yet been {@link #set()}
         */
        static final int UNSET = -1;

        /**
         * Sets this "Offset" to the offset of the current inserter; inserts this "Offset" before the current inserter.
         */
        public void
        set() {
            if (this.offset != Offset.UNSET) {
                throw new InternalCompilerException("Cannot \"set()\" Offset more than once");
            }

            Inserter ci = CodeContext.this.currentInserter;

            this.offset = ci.offset;

            Offset cip = ci.prev;
            assert cip != null;

            this.prev = cip;
            this.next = ci;

            cip.next = this;
            ci.prev  = this;
        }

        /**
         * @return The {@link CodeContext} that this {@link Offset} belongs to
         */
        public final CodeContext getCodeContext() { return CodeContext.this; }

        @Override public String
        toString() { return CodeContext.this.classFile.getThisClassName() + ": " + this.offset; }
    }

    /**
     * Adds another entry to the "exception_table" of this code attribute (see JVMS 4.7.3).
     *
     * @param catchTypeFd {@code null} means {@code finally} clause
     */
    public void
    addExceptionTableEntry(Offset startPc, Offset endPc, Offset handlerPc, @Nullable String catchTypeFd) {
        this.exceptionTableEntries.add(new ExceptionTableEntry(
            startPc,
            endPc,
            handlerPc,
            catchTypeFd == null ? (short) 0 : this.classFile.addConstantClassInfo(catchTypeFd)
        ));
    }

    /**
     * Representation of an entry in the "exception_table" of a "Code" attribute (see JVMS 4.7.3).
     */
    private static
    class ExceptionTableEntry {
        ExceptionTableEntry(Offset startPc, Offset endPc, Offset handlerPc, short  catchType) {
            this.startPC   = startPc;
            this.endPC     = endPc;
            this.handlerPC = handlerPc;
            this.catchType = catchType;
        }
        final Offset startPC, endPC, handlerPC;
        final short  catchType; // 0 == "finally" clause
    }

    /**
     * A class that implements an insertion point into a "Code" attribute.
     */
    public
    class Inserter extends Offset {
        @Nullable private Inserter nextInserter; // null == not in "currentInserter" stack
    }

    /**
     * An {@link Offset} who's sole purpose is to later create a 'LineNumberTable' attribute.
     */
    public
    class LineNumberOffset extends Offset {

        private final short lineNumber;

        /**
         * @param lineNumber 1...65535
         */
        public
        LineNumberOffset(int offset, short lineNumber) {
            this.lineNumber = lineNumber;
            this.offset     = offset;
        }
    }

    private abstract
    class Relocatable {

        /**
         * Relocates this object.
         *
         * @return {@code true} if the relocation succeeded in place; {@code false} if the relocation grew the number
         *         of bytes required
         */
        public abstract boolean relocate();
    }

    /**
     * A throw-in interface that marks {@link CodeContext.Offset}s as "fix-ups": During the execution of {@link
     * CodeContext#fixUp}, all "fix-ups" are invoked and can do last touches to the code attribute.
     * <p>
     *   This is currently used for inserting the "padding bytes" into the TABLESWITCH and LOOKUPSWITCH instructions.
     * </p>
     */
    public
    interface FixUp {

        /**
         * @see FixUp
         */
        void fixUp();
    }

    /**
     * @return All the local variables that are allocated in any block in this {@link CodeContext}
     */
    public List<Java.LocalVariableSlot>
    getAllLocalVars() { return this.allLocalVars; }

    /**
     * Removes all code between <var>from</var> and <var>to</var>. Also removes any {@link CodeContext.Relocatable}s
     * existing in that range.
     */
    public void
    removeCode(Offset from, Offset to) {

        if (from == to) return;

        int size = to.offset - from.offset;
        assert size >= 0;

        if (size == 0) return; // Short circuit.

        // Shift down the bytecode past "to".
        System.arraycopy(this.code, to.offset, this.code, from.offset, this.end.offset - to.offset);

        // Invalidate all offsets between "from" and "to".
        // Remove all relocatables that originate between "from" and "to".
        Set<Offset> invalidOffsets = new HashSet<Offset>();
        {
            Offset o = from.next;
            assert o != null;

            for (; o != to;) {
                assert o != null;

                invalidOffsets.add(o);

                // Invalidate the offset for fast failure.
                final Offset n = o.next;
                o.offset    = -77;
                o.prev      = null;
                o.next      = null;

                o = n;
                assert o != null;
            }

            for (;;) {
                o.offset -= size;
                if (o == this.end) break;
                o = o.next;
                assert o != null;
            }
        }

        // Invalidate all relocatables which originate or target a removed offset.
        for (Iterator<Relocatable> it = this.relocatables.iterator(); it.hasNext();) {
            Relocatable r = (Relocatable) it.next();

            if (r instanceof Branch) {
                Branch b = (Branch) r;

                if (invalidOffsets.contains(b.source)) {
                    it.remove();
                } else {
                    assert !invalidOffsets.contains(b.destination);
                }
            }

            if (r instanceof OffsetBranch) {
                OffsetBranch ob = (OffsetBranch) r;

                if (invalidOffsets.contains(ob.source)) {
                    it.remove();
                } else {
                    assert !invalidOffsets.contains(ob.destination);
                }
            }
        }

        for (Iterator<ExceptionTableEntry> it = this.exceptionTableEntries.iterator(); it.hasNext();) {
            ExceptionTableEntry ete = (ExceptionTableEntry) it.next();

            // Start, end and handler must either ALL lie IN the range to remove or ALL lie outside.

            if (invalidOffsets.contains(ete.startPC)) {
                assert invalidOffsets.contains(ete.endPC);
                assert invalidOffsets.contains(ete.handlerPC);
                it.remove();
            } else {
                assert !invalidOffsets.contains(ete.endPC);
                assert !invalidOffsets.contains(ete.handlerPC);
            }
        }

        // Remove local variables in dead-code block.
        for (Iterator<LocalVariableSlot> it = this.allLocalVars.iterator(); it.hasNext();) {
            final LocalVariableSlot var = (LocalVariableSlot) it.next();
            if (invalidOffsets.contains(var.getStart())) {
                assert invalidOffsets.contains(var.getEnd());
                it.remove();
            } else {
                assert !invalidOffsets.contains(var.getEnd());
            }
        }

        from.next = to;
        to.prev   = from;
    }
}
