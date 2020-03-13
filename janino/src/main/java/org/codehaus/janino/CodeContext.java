
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2010 Arno Unkrig. All rights reserved.
 * Copyright (c) 2015-2016 TIBCO Software Inc. All rights reserved. // CHECKSTYLE:OFF CHECKSTYLE:ON
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

import org.codehaus.commons.compiler.InternalCompilerException;
import org.codehaus.commons.nullanalysis.Nullable;
import org.codehaus.janino.util.ClassFile;
import org.codehaus.janino.util.ClassFile.AttributeInfo;
import org.codehaus.janino.util.ClassFile.ConstantClassInfo;
import org.codehaus.janino.util.ClassFile.LineNumberTableAttribute.Entry;
import org.codehaus.janino.util.ClassFile.StackMapTableAttribute;
import org.codehaus.janino.util.ClassFile.StackMapTableAttribute.FullFrame;
import org.codehaus.janino.util.ClassFile.StackMapTableAttribute.ObjectVariableInfo;
import org.codehaus.janino.util.ClassFile.StackMapTableAttribute.StackMapFrame;
import org.codehaus.janino.util.ClassFile.StackMapTableAttribute.UninitializedVariableInfo;
import org.codehaus.janino.util.ClassFile.StackMapTableAttribute.VerificationTypeInfo;

/**
 * The context of the compilation of a function (constructor or method). Manages generation of byte code, the exception
 * table, generation of line number tables, allocation of local variables, determining of stack size and local variable
 * table size and flow analysis.
 */
public
class CodeContext {

    private static final int     INITIAL_SIZE   = 128;

    private final ClassFile classFile;

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
    @Nullable private LocalScope currentLocalScope;

    static
    class LocalScope {

        @Nullable final LocalScope         parent;
        final short                        startingLocalVariableSlot;
        final List<Java.LocalVariableSlot> localVars = new ArrayList<Java.LocalVariableSlot>();
        final StackMap                     startingStackMap;

        LocalScope(@Nullable LocalScope parent, short startingLocalSlot, StackMap startingStackMap) {
            this.parent                    = parent;
            this.startingLocalVariableSlot = startingLocalSlot;
            this.startingStackMap          = startingStackMap;
        }
    }

    private short                   nextLocalVariableSlot;
    private final List<Relocatable> relocatables = new ArrayList<Relocatable>();

    /**
     * Creates an empty "Code" attribute.
     */
    public
    CodeContext(ClassFile classFile, IClass[] parameterTypes) {
        this.classFile = classFile;

        this.maxStack               = 0;
        this.maxLocals              = 0;
        this.code                   = new byte[CodeContext.INITIAL_SIZE];
        this.exceptionTableEntries  = new ArrayList<ExceptionTableEntry>();

        this.beginning              = new Offset();
        this.beginning.offset       = 0;

        this.currentInserter        = new Inserter();
        this.currentInserter.offset = 0;
        this.currentInserter.setStackMap(new StackMap(new VerificationTypeInfo[0], new VerificationTypeInfo[0]));

        this.beginning.next         = this.currentInserter;
        this.currentInserter.prev   = this.beginning;

        this.end                    = this.currentInserter;
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

        LocalScope currentScope = this.currentLocalScope;
        assert currentScope != null : "saveLocalVariables must be called first";

        final List<Java.LocalVariableSlot> currentVars = currentScope.localVars;

        Java.LocalVariableSlot slot = new Java.LocalVariableSlot(name, this.nextLocalVariableSlot, type);

        if (name != null) {
            slot.setStart(this.newOffset());
        }

//        // This check would be wrong, because one (Long|Double)_variable_info maps TWO local variable slots.
//        if (this.nextLocalVariableSlot != this.currentInserter.stackMap.locals().length) {
//            throw new InternalCompilerException(...);
//        }

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

        // Push empty list on the local scope stack to hold a new block's local vars.
        return (this.currentLocalScope = new LocalScope(
            this.currentLocalScope,
            this.nextLocalVariableSlot,
            this.currentInserter.getStackMap()
        )).localVars;
    }

    /**
     * Restores the previous size of the local variables array. This MUST to be called for every call to
     * saveLocalVariables as it closes the variable extent for all the active local variables in the current block.
     */
    public void
    restoreLocalVariables() {

        // Pop the list containing the current block's local vars.
        LocalScope scopeToPop = this.currentLocalScope;
        assert scopeToPop != null;

        for (Java.LocalVariableSlot slot : scopeToPop.localVars) {
            if (slot.getName() != null) slot.setEnd(this.newOffset());
        }

        this.currentLocalScope = scopeToPop.parent;

        // Do NOT restore the stack map because that would be incorrect, e.g. if the sets a previously unassigned
        // local variable.
//        this.currentInserter.setStackMap(scopeToPop.startingStackMap);

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
            dos.writeShort(exceptionTableEntry.startPc.offset);
            dos.writeShort(exceptionTableEntry.endPc.offset);
            dos.writeShort(exceptionTableEntry.handlerPc.offset);
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

        // Add the "StackMapTable" attribute.
        {

            List<StackMapFrame> smfs = new ArrayList<ClassFile.StackMapTableAttribute.StackMapFrame>();
            {
                int previousOffset = -1;
                for (Offset o = this.beginning; o != null && o.offset != this.end.offset; o = o.next) {

                    if (o.offset == 0) continue;

                    // "Padder" is used to insert the padding bytes of the LOOKUPSWITCH instruction; skip it, because
                    // it sits INSIDE the instruction and would cause a "StackMapTable error: bad offset".
                    if (o instanceof Java.Padder) continue;

                    // "FourByteOffset" is used to write the offsets of the LOOKUPSWITCH instruction. Skip it, because
                    // it sits INSIDE the instruction and would cause a "StackMapTable error: bad offset".
                    if (o instanceof FourByteOffset) continue;

                    {
                        Offset next = o.next;
                        if (next != null && o.offset == next.offset) continue;
                    }

                    smfs.add(new FullFrame(
                        o.offset - previousOffset - 1, // offset_delta
                        o.getStackMap().locals(),      // locals
                        o.getStackMap().operands()     // stack
                    ));
                    previousOffset = o.offset;
                }
            }

            attributes.add(
                new StackMapTableAttribute(
                    stackMapTableAttributeNameIndex,
                    (StackMapFrame[]) smfs.toArray(new StackMapFrame[smfs.size()])
                )
            );
        }

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
     * Fixes up all of the offsets and relocate() all relocatables.
     */
    public void
    fixUpAndRelocate() {
        this.maybeGrow();
        this.fixUp();
        this.relocate();
    }

    /**
     * Grow the code if relocatables are required to.
     */
    private void
    maybeGrow() {
        for (Relocatable relocatable : this.relocatables) {
            relocatable.grow();
        }
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
     * Relocates all relocatables.
     */
    private void
    relocate() {
        for (Relocatable relocatable : this.relocatables) {
            relocatable.relocate();
        }
    }

    /**
     * Inserts a sequence of bytes at the current insertion position. Creates {@link LineNumberOffset}s as necessary.
     */
    public void
    write(byte[] b) {

        if (b.length == 0) return;

        // CAVEAT: "makeSpace()" may change "this.code", so make sure to call "makeSpace()" _before_ using "this.code"!
        int o = this.makeSpace(b.length);
        System.arraycopy(b, 0, this.code, o, b.length);
    }

    /**
     * Inserts a byte at the current insertion position. Creates {@link LineNumberOffset}s as necessary.
     * <p>
     *   This method is an optimization to avoid allocating small byte[] and ease GC load.
     * </p>
     */
    public void
    write(byte b1) {

        // CAVEAT: "makeSpace()" may change "this.code", so make sure to call "makeSpace()" _before_ using "this.code"!
        int o = this.makeSpace(1);
        this.code[o] = b1;
    }

    /**
     * Inserts bytes at the current insertion position. Creates {@link LineNumberOffset}s as necessary.
     * <p>
     *   This method is an optimization to avoid allocating small byte[] and ease GC load.
     * </p>
     */
    public void
    write(byte b1, byte b2) {

        int o = this.makeSpace(2);

        this.code[o++] = b1;
        this.code[o]   = b2;
    }

    /**
     * Inserts bytes at the current insertion position. Creates {@link LineNumberOffset}s as necessary.
     * <p>
     *   This method is an optimization to avoid allocating small byte[] and ease GC load.
     * </p>
     */
    public void
    write(byte b1, byte b2, byte b3) {

        int o = this.makeSpace(3);

        this.code[o++] = b1;
        this.code[o++] = b2;
        this.code[o]   = b3;
    }

    /**
     * Inserts bytes at the current insertion position. Creates {@link LineNumberOffset}s as necessary.
     * <p>
     *   This method is an optimization to avoid allocating small byte[] and ease GC load.
     * </p>
     */
    public void
    write(byte b1, byte b2, byte b3, byte b4) {

        int o = this.makeSpace(4);

        this.code[o++] = b1;
        this.code[o++] = b2;
        this.code[o++] = b3;
        this.code[o]   = b4;
    }

    public void
    addLineNumberOffset(int lineNumber) {

        if (lineNumber == -1) return;

        if (lineNumber > 0xffff) lineNumber = 0xffff;

        // Find out whether the line number is different from the line number of the preceding insertion,
        // and, if so, insert a LineNumberOffset object, which will later lead to a LineNumberTable entry.
        for (Offset o = this.currentInserter.prev; o != this.beginning; o = o.prev) {
            assert o != null;
            if (o instanceof LineNumberOffset) {
                if ((((LineNumberOffset) o).lineNumber & 0xffff) == lineNumber) return;
                break;
            }
        }

        // Insert a LineNumberOffset _before_ the current inserter.
        LineNumberOffset lno = new LineNumberOffset(this.currentInserter.offset, this.currentInserter.getStackMap(), (short) lineNumber);

        Offset cip = this.currentInserter.prev;
        assert cip != null;

        lno.prev = cip;
        lno.next = this.currentInserter;

        cip.next = lno;

        this.currentInserter.prev = lno;
    }

    /**
     * Inserts <var>size</var> NUL bytes at the current inserter's offset, advances the current inserter's offset by
     * <var>size</var>, creates {@link LineNumberOffset}s as necessary, and returns the current inserter's original
     * offset (the offset of the first NUL byte that was inserted).
     * <p>
     *   Because the class file format does not support line numbers greater than 65535, these are treated as 65535.
     * </p>
     *
     * @param size The number of NUL bytes to inject
     * @return     The offset of the first inserted byte
     */
    public int
    makeSpace(final int size) {

        final int cio = this.currentInserter.offset;

        if (size == 0) return cio;

        if (this.end.offset + size <= this.code.length) {

            // Optimization to avoid a trivial method call in the common case
            if (cio != this.end.offset) {
                System.arraycopy(this.code, cio, this.code, cio + size, this.end.offset - cio);
            }
        } else {
            byte[] oldCode = this.code;
            //double size to avoid horrible performance, but don't grow over our limit
            int newSize = Math.max(Math.min(oldCode.length * 2, 0xffff), oldCode.length + size);
            if (newSize > 0xffff) throw new InternalCompilerException("Code grows beyond 64 KB");
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
    writeShort(int v) { this.write((byte) (v >> 8), (byte) v); }

    /**
     * Generates a "branch" instruction.
     *
     * @param opcode One of {@link Opcode#GOTO}, {@link Opcode#JSR} and <code>Opcode.IF*</code>
     * @param dst    Where to branch
     */
    public void
    writeBranch(int opcode, final Offset dst) {

        if (dst.offset == -1) {
            if (dst.stackMap == null) {
                dst.stackMap = this.currentInserter.getStackMap();
            }
        }

        this.relocatables.add(new Branch(opcode, dst));
        this.write((byte) opcode, (byte) -1, (byte) -1);
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

        @Override public void
        grow() {
            if (this.destination.offset == Offset.UNSET) {
                throw new InternalCompilerException("Cannot relocate branch to unset destination offset");
            }
            int offset = this.destination.offset - this.source.offset;

            @SuppressWarnings("deprecation") final int opcodeJsr = Opcode.JSR;
            if (!this.expanded && (offset > Short.MAX_VALUE || offset < Short.MIN_VALUE)) {
                //we want to insert the data without skewing our source position,
                //so we will cache it and then restore it later.
                final int pos = this.source.offset;
                CodeContext.this.pushInserter(this.source);
                {
                    // Promotion to a wide instruction only requires 2 extra bytes. Everything else requires a new
                    // GOTO_W instruction after a negated if (5 extra bytes).
                    CodeContext.this.makeSpace(this.opcode == Opcode.GOTO || this.opcode == opcodeJsr ? 2 : 5);
                }
                CodeContext.this.popInserter();
                this.source.offset = pos;
                this.expanded      = true;
            }
        }

        @Override public void
        relocate() {
            if (this.destination.offset == Offset.UNSET) {
                throw new InternalCompilerException("Cannot relocate branch to unset destination offset");
            }
            int offset = this.destination.offset - this.source.offset;

            @SuppressWarnings("deprecation") final int opcodeJsr = Opcode.JSR;
            final byte[] ba;
            if (!this.expanded) {
                //we fit in a 16-bit jump
                ba = new byte[] { (byte) this.opcode, (byte) (offset >> 8), (byte) offset };
            } else {
                if (this.opcode == Opcode.GOTO || this.opcode == opcodeJsr) {
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
                        (byte) CodeContext.invertBranchOpcode(this.opcode),
                        0,
                        8, // Jump from this instruction past the GOTO_W
                        (byte) Opcode.GOTO_W,
                        (byte) (offset >> 24),
                        (byte) (offset >> 16),
                        (byte) (offset >> 8),
                        (byte) offset
                    };
                }
            }
            System.arraycopy(ba, 0, CodeContext.this.code, this.source.offset, ba.length);
        }

        private boolean        expanded; //marks whether this has been expanded to account for a wide branch
        private final int      opcode;
        private final Inserter source;
        private final Offset   destination;
    }

    /**
     * E.g. {@link Opcode#IFLT} ("less than") inverts to {@link Opcode#IFGE} ("greater than or equal to").
     */
    private static int
    invertBranchOpcode(int branchOpcode) {
        final Integer result = (Integer) CodeContext.BRANCH_OPCODE_INVERSION.get(Integer.valueOf(branchOpcode));
        assert result != null : branchOpcode;
        return result;
    }

    private static final Map<Integer /*branch-opcode*/, Integer /*inverted-branch-opcode*/>
    BRANCH_OPCODE_INVERSION = CodeContext.createBranchOpcodeInversion();
    private static Map<Integer, Integer>
    createBranchOpcodeInversion() {
        Map<Integer, Integer> m = new HashMap<Integer, Integer>();
        m.put(Opcode.IF_ACMPEQ, Opcode.IF_ACMPNE);
        m.put(Opcode.IF_ACMPNE, Opcode.IF_ACMPEQ);
        m.put(Opcode.IF_ICMPEQ, Opcode.IF_ICMPNE);
        m.put(Opcode.IF_ICMPNE, Opcode.IF_ICMPEQ);
        m.put(Opcode.IF_ICMPGE, Opcode.IF_ICMPLT);
        m.put(Opcode.IF_ICMPLT, Opcode.IF_ICMPGE);
        m.put(Opcode.IF_ICMPGT, Opcode.IF_ICMPLE);
        m.put(Opcode.IF_ICMPLE, Opcode.IF_ICMPGT);
        m.put(Opcode.IFEQ,      Opcode.IFNE);
        m.put(Opcode.IFNE,      Opcode.IFEQ);
        m.put(Opcode.IFGE,      Opcode.IFLT);
        m.put(Opcode.IFLT,      Opcode.IFGE);
        m.put(Opcode.IFGT,      Opcode.IFLE);
        m.put(Opcode.IFLE,      Opcode.IFGT);
        m.put(Opcode.IFNULL,    Opcode.IFNONNULL);
        m.put(Opcode.IFNONNULL, Opcode.IFNULL);
        return Collections.unmodifiableMap(m);
    }

    /**
     * Writes a four-byte offset (as it is used in TABLESWITCH and LOOKUPSWITCH) into this code context.
     */
    public void
    writeOffset(Offset src, final Offset dst) {
        Offset o = new FourByteOffset();
        o.set();

        this.relocatables.add(new OffsetBranch(o, src, dst));
        this.makeSpace(4);
    }
    private final class FourByteOffset extends Offset {}

    private
    class OffsetBranch extends Relocatable {

        OffsetBranch(Offset where, Offset source, Offset destination) {
            this.where       = where;
            this.source      = source;
            this.destination = destination;
        }

        @Override public void
        grow() {}

        @Override public void
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

        // The current inserter MUST have a stack map; verify.
        ins.getStackMap();

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
        ni.getStackMap();

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

        @Nullable private StackMap stackMap;     // Is null until "set()".

        /**
         * Sets this "Offset" to the offset of the current inserter; inserts this "Offset" before the current inserter.
         */
        public void
        set() {

            this.setOffset();

            this.setStackMap();

            Inserter ci = CodeContext.this.currentInserter;

            Offset cip = ci.prev;
            assert cip != null;

            this.prev = cip;
            this.next = ci;

            cip.next = this;
            ci.prev  = this;
        }

        /**
         * Merges the stack maps of the current inserter and THIS offset, and assigns the result to the current
         * inserter and THIS offset.
         */
        void
        setStackMap() {

            Inserter ci = CodeContext.this.currentInserter;

            final StackMap ciSm = ((Offset) ci).stackMap;
            if (ciSm == null) {

                // A very special case: The current inserter has no stack map -- this happens when a GOTO has just been
                // written.
                ((Offset) ci).stackMap = this.stackMap;
            } else {
                StackMap thisSm = this.stackMap;

                if (thisSm == null) {
                    this.stackMap = ciSm;
                } else
                if (ciSm == thisSm) {
                    ;
                } else
                if (ciSm.equals(thisSm)) {
                    this.stackMap = ciSm;
                } else
                {
                    if (!Arrays.equals(ciSm.operands(), thisSm.operands())) {
                        throw new InternalCompilerException("Inconsistent operand stack: " + ciSm + " vs. " + thisSm);
                    }
                    VerificationTypeInfo[] ciLocals   = ciSm.locals();
                    VerificationTypeInfo[] thisLocals = thisSm.locals();
                    VerificationTypeInfo[] tmp        = new VerificationTypeInfo[Math.min(ciLocals.length, thisLocals.length)];

                    for (int i = 0; i < tmp.length; i++) {
                        VerificationTypeInfo ciLocal   = ciLocals[i];
                        VerificationTypeInfo thisLocal = thisLocals[i];
                        VerificationTypeInfo tmpLocal;
                        if (ciLocal.equals(thisLocal)) {
                            tmpLocal = ciLocal;
                        } else
                        if (ciLocal == StackMapTableAttribute.TOP_VARIABLE_INFO || thisLocal == StackMapTableAttribute.TOP_VARIABLE_INFO) {
                            tmpLocal = StackMapTableAttribute.TOP_VARIABLE_INFO;
                        } else
                        {
                            // This check would be wrong, because the different flow paths may have used the slot
                            // for different locals.
//                            throw new InternalCompilerException(
//                                "Inconsistent local variable #" + i + " verification type: " + ciSm + " vs. " + thisSm
//                            );
                            tmpLocal = StackMapTableAttribute.TOP_VARIABLE_INFO;
                        }

                        tmp[i] = tmpLocal;
                    }

                    StackMap newSm = new StackMap(tmp, ciSm.operands());
                    ci.setStackMap(newSm);
                    this.stackMap = newSm;
                }
            }
        }

        public void
        setOffset() {
            Inserter ci = CodeContext.this.currentInserter;

            if (this.offset != Offset.UNSET) throw new InternalCompilerException("Offset already set");
            this.offset = ci.offset;
        }

        public StackMap
        getStackMap() {
            final StackMap result = this.stackMap;
            if (result == null) {
                throw new InternalCompilerException("Stack map not set");
            }
            return result;
        }

        public void
        setStackMap(StackMap stackMap) { this.stackMap = stackMap; }

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
            this.startPc   = startPc;
            this.endPc     = endPc;
            this.handlerPc = handlerPc;
            this.catchType = catchType;
        }
        final Offset startPc, endPc, handlerPc;
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
        LineNumberOffset(int offset, StackMap stackMap, short lineNumber) {
            this.lineNumber = lineNumber;
            this.offset     = offset;
            this.setStackMap(stackMap);
        }
    }

    private abstract
    class Relocatable {

        /**
         * Grows the code if the relocation cannot be done without growing code.
         */
        public abstract void grow();

        /**
         * Relocates this object.
         */
        public abstract void relocate();
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

            if (invalidOffsets.contains(ete.startPc)) {
                assert invalidOffsets.contains(ete.endPc);
                assert invalidOffsets.contains(ete.handlerPc);
                it.remove();
            } else {
                assert !invalidOffsets.contains(ete.endPc);
                assert !invalidOffsets.contains(ete.handlerPc);
            }
        }

        // Remove local variables in dead-code block.
        for (Iterator<Java.LocalVariableSlot> it = this.allLocalVars.iterator(); it.hasNext();) {
            final Java.LocalVariableSlot var = (Java.LocalVariableSlot) it.next();
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

    @Override public String
    toString() { return this.classFile.getThisClassName() + "/" + this.currentInserter.offset; }

    // Convenience methods for "pushOperand(VTI)".

    /**
     * Pushes one {@code object_variable_info}, {@code integer_variable_info}, {@code double_variable_info}, {@code
     * float_variable_info} or {@code long_variable_info} entry onto the current inserter's operand stack.
     */
    public void
    pushOperand(String fieldDescriptor) {

        if (Descriptor.isReference(fieldDescriptor)) {
            this.pushObjectOperand(fieldDescriptor);
        } else
        if (
            fieldDescriptor.equals(Descriptor.BYTE)
            || fieldDescriptor.equals(Descriptor.CHAR)
            || fieldDescriptor.equals(Descriptor.INT)
            || fieldDescriptor.equals(Descriptor.SHORT)
            || fieldDescriptor.equals(Descriptor.BOOLEAN)
        ) {
            this.pushIntOperand();
        } else
        if (fieldDescriptor.equals(Descriptor.DOUBLE)) {
            this.pushDoubleOperand();
        } else
        if (fieldDescriptor.equals(Descriptor.FLOAT)) {
            this.pushFloatOperand();
        } else
        if (fieldDescriptor.equals(Descriptor.LONG)) {
            this.pushLongOperand();
        } else
        {
            throw new AssertionError(fieldDescriptor);
        }
    }

    public void pushTopOperand()               { this.pushOperand(StackMapTableAttribute.TOP_VARIABLE_INFO);                }
    public void pushIntOperand()               { this.pushOperand(StackMapTableAttribute.INTEGER_VARIABLE_INFO);            }
    public void pushLongOperand()              { this.pushOperand(StackMapTableAttribute.LONG_VARIABLE_INFO);               }
    public void pushFloatOperand()             { this.pushOperand(StackMapTableAttribute.FLOAT_VARIABLE_INFO);              }
    public void pushDoubleOperand()            { this.pushOperand(StackMapTableAttribute.DOUBLE_VARIABLE_INFO);             }
    public void pushNullOperand()              { this.pushOperand(StackMapTableAttribute.NULL_VARIABLE_INFO);               }
    public void pushUninitializedThisOperand() { this.pushOperand(StackMapTableAttribute.UNINITIALIZED_THIS_VARIABLE_INFO); }

    public void
    pushUninitializedOperand() {

        final Offset                    o   = this.newOffset();
        final UninitializedVariableInfo uvi = this.classFile.newUninitializedVariableInfo((short) o.offset);

        this.relocatables.add(new Relocatable() {

            @Override public void
            grow() {}

            @Override public void
            relocate() { uvi.offset = (short) o.offset; }
        });

        this.pushOperand(uvi);
    }

    public void
    pushObjectOperand(String fieldDescriptor) {
        this.pushOperand(this.classFile.newObjectVariableInfo(fieldDescriptor));
    }

    public void
    pushOperand(VerificationTypeInfo topOperand) {
        final Inserter ci = this.currentInserter();

        StackMap sm = ci.getStackMap();
        sm = sm.pushOperand(topOperand);
        ci.setStackMap(sm);

        int ss = 0;
        for (VerificationTypeInfo vti : sm.operands()) ss += vti.category();
        if (ss > this.maxStack) this.maxStack = ss;
    }

    /**
     * @return Whether the top operand is a {@code null_variable_info}
     */
    public boolean
    peekNullOperand() {
        return this.peekOperand() == StackMapTableAttribute.NULL_VARIABLE_INFO;
    }

    /**
     * @return Whether the top operand is a {@code object_variable_info}
     */
    public boolean
    peekObjectOperand() {
        return this.peekOperand() instanceof StackMapTableAttribute.ObjectVariableInfo;
    }

    /**
     * @return The verification type of the top operand
     */
    public VerificationTypeInfo
    peekOperand() { return this.currentInserter().getStackMap().peekOperand(); }

    /**
     * Pops one entry from the current inserter's operand stack.
     */
    public VerificationTypeInfo
    popOperand() {

        Inserter ci = this.currentInserter();
        StackMap sm = ci.getStackMap();

        VerificationTypeInfo result = sm.peekOperand();

        ci.setStackMap(sm.popOperand());

        return result;
    }

    /**
     * Pops the top entry from the operand stack and assert that it equals <var>expected</var>.
     */
    public void
    popOperand(VerificationTypeInfo expected) {
        VerificationTypeInfo actual = this.popOperand();
        assert actual.equals(expected) : actual;
    }

    /**
     * Pops the top operand, asserts that it is an {@code integer_variable_info}, {@code long_variable_info}, {@code
     * float_variable_info}, {@code double_variable_info} or {@code variable_object_info}, and asserts that it matches
     * the given field descriptor.
     */
    public void
    popOperand(String expectedFd) {

        VerificationTypeInfo vti = this.popOperand();

        if (vti == StackMapTableAttribute.INTEGER_VARIABLE_INFO) {
            assert (
                expectedFd.equals(Descriptor.BOOLEAN)
                || expectedFd.equals(Descriptor.BYTE)
                || expectedFd.equals(Descriptor.CHAR)
                || expectedFd.equals(Descriptor.SHORT)
                || expectedFd.equals(Descriptor.INT)
            ) : expectedFd;
        } else
        if (vti == StackMapTableAttribute.LONG_VARIABLE_INFO) {
            assert expectedFd.equals(Descriptor.LONG) : expectedFd;
        } else
        if (vti == StackMapTableAttribute.FLOAT_VARIABLE_INFO) {
            assert expectedFd.equals(Descriptor.FLOAT) : expectedFd;
        } else
        if (vti == StackMapTableAttribute.DOUBLE_VARIABLE_INFO) {
            assert expectedFd.equals(Descriptor.DOUBLE) : expectedFd;
        } else
        if (vti == StackMapTableAttribute.NULL_VARIABLE_INFO) {
            assert expectedFd.equals(Descriptor.VOID) : expectedFd;
        } else
        if (vti instanceof StackMapTableAttribute.ObjectVariableInfo) {
            assert Descriptor.isReference(expectedFd) : expectedFd;

            final ObjectVariableInfo ovi = (StackMapTableAttribute.ObjectVariableInfo) vti;
            final ConstantClassInfo  cci = this.classFile.getConstantClassInfo(ovi.getConstantClassInfoIndex());

            final String computationalTypeFd = Descriptor.fromInternalForm(cci.getName(this.classFile));
            assert expectedFd.equals(computationalTypeFd) : expectedFd + " vs. " + computationalTypeFd;
        } else
        if (vti instanceof StackMapTableAttribute.UninitializedVariableInfo) {
            assert Descriptor.isReference(expectedFd) : expectedFd;
        } else
        {
            throw new AssertionError(vti);
        }
    }

    public void
    popOperandAssignableTo(String declaredFd) {

        if (Descriptor.isPrimitive(declaredFd)) {
            this.popOperand(declaredFd);
        } else {
            this.popObjectOperand();
        }
    }

    /**
     * Asserts that the top operand is an {@code integer_variable_info} and pops it.
     */
    public void
    popIntOperand() { this.popOperand(StackMapTableAttribute.INTEGER_VARIABLE_INFO); }

    /**
     * Asserts that the top operand is a {@code long_variable_info} and pops it.
     */
    public void
    popLongOperand() { this.popOperand(StackMapTableAttribute.LONG_VARIABLE_INFO); }

    /**
     * Asserts that the top operand is an {@code uninitializedThis_variable_info} and pops it.
     */
    public void
    popUninitializedThisOperand() { this.popOperand(StackMapTableAttribute.UNINITIALIZED_THIS_VARIABLE_INFO); }

    /**
     * Asserts that the top operand is an {@code uninitialized_variable_info} and pops it.
     */
    public void
    popUninitializedVariableOperand() {
        VerificationTypeInfo result = this.popOperand();
        assert result instanceof StackMapTableAttribute.UninitializedVariableInfo;
    }

    /**
     * Asserts that the top operand is an {@code object_variable_info} or a {@code null_variable_info} and pops it.
     */
    public void
    popReferenceOperand() {
        assert this.peekObjectOperand() || this.peekNullOperand() : this.peekOperand();
        this.popOperand();
    }

    /**
     * Asserts that the top operand is an {@code object_variable_info}, and pops it.
     *
     * @return The field descriptor of the popped object operand
     */
    public String
    popObjectOperand() {

        VerificationTypeInfo vti = this.popOperand();
        assert vti instanceof StackMapTableAttribute.ObjectVariableInfo : vti;
        final ObjectVariableInfo ovi = (StackMapTableAttribute.ObjectVariableInfo) vti;

        short             ccii = ovi.getConstantClassInfoIndex();
        ConstantClassInfo cci  = this.classFile.getConstantClassInfo(ccii);
        return Descriptor.fromInternalForm(cci.getName(this.classFile));
    }

    /**
     * Asserts that the top operand is an {@code object_variable_info}, {@code uninitialized_variable_info} or
     * {@code uninitializedThis_variable_info}, and pops it.
     */
    public VerificationTypeInfo
    popObjectOrUninitializedOrUninitializedThisOperand() {
        VerificationTypeInfo result = this.popOperand();
        assert (
            result instanceof StackMapTableAttribute.UninitializedVariableInfo
            || result instanceof StackMapTableAttribute.ObjectVariableInfo
            || result == StackMapTableAttribute.UNINITIALIZED_THIS_VARIABLE_INFO
        ) : result;
        return result;
    }

    /**
     * Asserts that the top operand is an {@code int_variable_info} or {@code long_variable_info}, then pops and
     * returns it.
     */
    public VerificationTypeInfo
    popIntOrLongOperand() {
        VerificationTypeInfo result = this.popOperand();
        assert (
            result == StackMapTableAttribute.INTEGER_VARIABLE_INFO
            || result == StackMapTableAttribute.LONG_VARIABLE_INFO
        ) : result;
        return result;
    }
}
