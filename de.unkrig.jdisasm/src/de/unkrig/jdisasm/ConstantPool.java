
/*
 * JDISASM - A Java[TM] class file disassembler
 *
 * Copyright (c) 2001, Arno Unkrig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *       following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *       following disclaimer in the documentation and/or other materials provided with the distribution.
 *    3. The name of the author may not be used to endorse or promote products derived from this software without
 *       specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package de.unkrig.jdisasm;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import de.unkrig.commons.nullanalysis.Nullable;
import de.unkrig.jdisasm.SignatureParser.SignatureException;

/** Representation of the "constant pool" in a Java&trade; class file. */
public
class ConstantPool {

    /** Representation of a constant pool entry. */
    public
    interface ConstantPoolEntry {
    }

    /** Representation of a CONSTANT_Class_info entry. */
    public static
    class ConstantClassInfo implements ConstantPoolEntry {

        /** Fully qualified (dot-separated) class name. */
        public final String name;

        public ConstantClassInfo(String name) { this.name = name; }

        @Override public String
        toString() {
            return this.name + ".class";
        }
    }

    /** Representation of a CONSTANT_Fieldref_info entry. */
    public static
    class ConstantFieldrefInfo implements ConstantPoolEntry {

        /** {@code CONSTANT_Fieldref_info.class_index}, see JVMS7 4.4.2 */
        public final ConstantClassInfo clasS;

        /** {@code CONSTANT_Fieldref_info.name_and_type_index}, see JVMS7 4.4.2 */
        public final ConstantNameAndTypeInfo nameAndType;

        public
        ConstantFieldrefInfo(ConstantClassInfo clasS, ConstantNameAndTypeInfo nameAndType) {
            this.clasS       = clasS;
            this.nameAndType = nameAndType;
        }

        @Override public String
        toString() {
            try {
                return (
                    this.clasS.name
                    + "::"
                    + SignatureParser.decodeFieldDescriptor(this.nameAndType.descriptor.toString())
                    + " "
                    + this.nameAndType.name
                );
            } catch (SignatureException e) {
                return this.clasS.name + "::" + this.nameAndType;
            }
        }
    }

    /** Representation of a CONSTANT_Methodref_info entry. */
    public static
    class ConstantMethodrefInfo implements ConstantPoolEntry {

        /** {@code CONSTANT_Methodref_info.class_index}, see JVMS7 4.4.2 */
        public final ConstantClassInfo clasS;

        /** {@code CONSTANT_Methodref_info.name_and_type_index}, see JVMS7 4.4.2 */
        public final ConstantNameAndTypeInfo nameAndType;

        public
        ConstantMethodrefInfo(ConstantClassInfo clasS, ConstantNameAndTypeInfo nameAndType) {
            this.clasS       = clasS;
            this.nameAndType = nameAndType;
        }

        @Override public String
        toString() {
            try {
                return (
                    this.clasS.name
                    + "::"
                    + SignatureParser.decodeMethodDescriptor(this.nameAndType.descriptor.toString()).toString(
                        this.clasS.name,
                        this.nameAndType.name.toString()
                    )
                );
            } catch (SignatureException e) {
                return this.clasS.name + "::" + this.nameAndType;
            }
        }
    }

    /** Representation of a CONSTANT_InterfaceMethodref_info entry. */
    public static
    class ConstantInterfaceMethodrefInfo implements ConstantPoolEntry {

        /** {@code CONSTANT_InterfaceMethodref_info.class_index}, see JVMS7 4.4.2 */
        public final ConstantClassInfo clasS;

        /** {@code CONSTANT_InterfaceMethodref_info.name_and_type_index}, see JVMS7 4.4.2 */
        public final ConstantNameAndTypeInfo nameAndType;

        public
        ConstantInterfaceMethodrefInfo(ConstantClassInfo clasS, ConstantNameAndTypeInfo nameAndType) {
            this.clasS       = clasS;
            this.nameAndType = nameAndType;
        }

        @Override public String
        toString() {
            try {
                return (
                    this.clasS.name
                    + ":::"
                    + SignatureParser.decodeMethodDescriptor(this.nameAndType.descriptor.toString()).toString(
                        this.clasS.name,
                        this.nameAndType.name.toString()
                    )
                );
            } catch (SignatureException e) {
                return this.clasS.name + ":::" + this.nameAndType;
            }
        }
    }

    /** Representation of a CONSTANT_String_info entry. */
    public static
    class ConstantStringInfo implements ConstantPoolEntry {

        /** {@code CONSTANT_String_info.string_index}, see JVMS7 4.4.3 */
        public final String string;

        public
        ConstantStringInfo(String string) { this.string = string; }

        @Override public String
        toString() { return ConstantPool.stringToJavaLiteral(this.string); }
    }

    /** Representation of a CONSTANT_Integer_info entry. */
    public static
    class ConstantIntegerInfo implements ConstantPoolEntry {

        /** {@code CONSTANT_Integer_info.bytes}, see JVMS7 4.4.4 */
        public int bytes;

        @Override public String
        toString() {
            return Integer.toString(this.bytes);
        }
    }

    /** Representation of a CONSTANT_Float_info entry. */
    public static
    class ConstantFloatInfo implements ConstantPoolEntry {

        /** {@code CONSTANT_Float_info.bytes}, see JVMS7 4.4.4 */
        public float bytes;

        @Override public String
        toString() {
            return this.bytes + "F";
        }
    }

    /** Representation of a CONSTANT_Long_info entry. */
    public static
    class ConstantLongInfo implements ConstantPoolEntry {

        /** {@code CONSTANT_Long_info.bytes}, see JVMS7 4.4.5 */
        public long bytes;

        @Override public String
        toString() {
            return this.bytes + "L";
        }
    }

    /** Representation of a CONSTANT_Double_info entry. */
    public static
    class ConstantDoubleInfo implements ConstantPoolEntry {

        /** {@code CONSTANT_Double_info.bytes}, see JVMS7 4.4.5 */
        public double bytes;

        @Override public String
        toString() {
            return this.bytes + "D";
        }
    }

    /** Representation of a CONSTANT_NameAndType_info entry. */
    public static
    class ConstantNameAndTypeInfo implements ConstantPoolEntry {

        /** {@code CONSTANT_NameAndType_info.name_index}, see JVMS7 4.4.6 */
        public final ConstantUtf8Info name;

        /** {@code CONSTANT_NameAndType_info.descriptor_index}, see JVMS7 4.4.6 */
        public final ConstantUtf8Info descriptor;

        public
        ConstantNameAndTypeInfo(ConstantUtf8Info name, ConstantUtf8Info descriptor) {
            this.name       = name;
            this.descriptor = descriptor;
        }

        @Override public String
        toString() {
            return this.name + " : " + this.descriptor;
        }
    }

    /** Representation of a CONSTANT_Utf8_info entry. */
    public static
    class ConstantUtf8Info implements ConstantPoolEntry {

        /** {@code CONSTANT_Utf8_info.bytes}, see JVMS7 4.4.7 */
        public final String bytes;

        public
        ConstantUtf8Info(String bytes) { this.bytes = bytes; }

        @Override public String
        toString() {
            return this.bytes;
        }
    }

    /** The entries of this pool, as read from a class file by {@link #ConstantPool}. */
    final ConstantPoolEntry[] entries;

    /**
     * Reads a constant pool from the given {@link InputStream}. Afterwards, entries can be retrieved by invoking
     * the {@code getConstant*Info()} method family.
     *
     * @throws ClassCastException             An entry has the "wrong" type
     * @throws NullPointerException           An "unusable" entry (the magic "zero" entry and the entries after a LONG
     *                                        or DOUBLE entry) is referenced
     * @throws ArrayIndexOutOfBoundsException An index is too small or to great
     */
    public
    ConstantPool(DataInputStream dis) throws IOException {
        final int count = 0xffff & dis.readShort();

        // Read the entries into a temporary data structure - this is necessary because there may be forward
        // references.

        /***/
        abstract
        class RawEntry {

            /** Creates a 'cooked' entry from a 'raw' entry. */
            abstract ConstantPoolEntry
            cook();

            /** Returns the {@link ConstantClassInfo} entry with the given {@code index}. */
            ConstantClassInfo
            getConstantClassInfo(short index) { return (ConstantClassInfo) this.get(index); }

            /** Returns the {@link ConstantNameAndTypeInfo} entry with the given {@code index}. */
            ConstantNameAndTypeInfo
            getConstantNameAndTypeInfo(short index) { return (ConstantNameAndTypeInfo) this.get(index); }

            /** Returns the {@link ConstantUtf8Info} entry with the given {@code index}. */
            ConstantUtf8Info
            getConstantUtf8Info(short index) { return (ConstantUtf8Info) this.get(index); }

            /** Returns the {@link ConstantPoolInfo} entry with the given {@code index}. */
            abstract ConstantPoolEntry
            get(short index);
        }
        final RawEntry[] rawEntries = new RawEntry[count];

        /** Must declare a second 'RawEntry' class, because it references the local variable 'rawEntries'. */
        abstract
        class RawEntry2 extends RawEntry {

            @Override ConstantPoolEntry
            get(short index) {
                if (ConstantPool.this.entries[0xffff & index] == null) {
                    ConstantPool.this.entries[0xffff & index] = new ConstantPoolEntry() {
                        @Override @Nullable public String toString() { return null; }
                    }; // To prevent recursion.
                    ConstantPool.this.entries[0xffff & index] = rawEntries[0xffff & index].cook();
                }
                return ConstantPool.this.entries[0xffff & index];
            }
        }

        for (int i = 1; i < count;) {
            int      idx = i; // SUPPRESS CHECKSTYLE UsageDistance
            RawEntry re;
            byte     tag = dis.readByte();
            switch (tag) {
            case 7: // CONSTANT_Class_info
                {
                    final short nameIndex = dis.readShort();
                    re = new RawEntry2() {

                        @Override ConstantPoolEntry
                        cook() {
                            return new ConstantClassInfo(this.getConstantUtf8Info(nameIndex).bytes.replace('/', '.'));
                        }
                    };
                    i++;
                    break;
                }
            case 9: // CONSTANT_Fieldref_info
                {
                    final short classIndex       = dis.readShort();
                    final short nameAndTypeIndex = dis.readShort();
                    re = new RawEntry2() {

                        @Override ConstantPoolEntry
                        cook() {
                            return new ConstantFieldrefInfo(
                                this.getConstantClassInfo(classIndex),
                                this.getConstantNameAndTypeInfo(nameAndTypeIndex)
                            );
                        }
                    };
                    i++;
                    break;
                }
            case 10: // CONSTANT_Methodref_info
                {
                    final short classIndex       = dis.readShort();
                    final short nameAndTypeIndex = dis.readShort();
                    re = new RawEntry2() {

                        @Override ConstantPoolEntry
                        cook() {
                            return new ConstantMethodrefInfo(
                                this.getConstantClassInfo(classIndex),
                                this.getConstantNameAndTypeInfo(nameAndTypeIndex)
                            );
                        }
                    };
                    i++;
                    break;
                }
            case 11: // CONSTANT_InterfaceMethodref_info
                {
                    final short classIndex       = dis.readShort();
                    final short nameAndTypeIndex = dis.readShort();
                    re = new RawEntry2() {

                        @Override ConstantPoolEntry
                        cook() {
                            return new ConstantInterfaceMethodrefInfo(
                                this.getConstantClassInfo(classIndex),
                                this.getConstantNameAndTypeInfo(nameAndTypeIndex)
                            );
                        }
                    };
                    i++;
                    break;
                }
            case 8: // CONSTANT_String_info
                {
                    final short stringIndex = dis.readShort();
                    re = new RawEntry2() {

                        @Override ConstantPoolEntry
                        cook() {
                            return new ConstantStringInfo(this.getConstantUtf8Info(stringIndex).bytes);
                        }
                    };
                    i++;
                    break;
                }
            case 3: // CONSTANT_Integer_info
                {
                    final int byteS = dis.readInt();
                    re = new RawEntry2() {

                        @Override ConstantPoolEntry
                        cook() {
                            return new ConstantIntegerInfo() { {
                                this.bytes = byteS;
                            } };
                        }
                    };
                    i++;
                    break;
                }
            case 4: // CONSTANT_Float_info
                {
                    final float byteS = dis.readFloat();
                    re = new RawEntry2() {

                        @Override ConstantPoolEntry
                        cook() {
                            return new ConstantFloatInfo() { {
                                this.bytes = byteS;
                            } };
                        }
                    };
                    i++;
                    break;
                }
            case 5: // CONSTANT_Long_info
                {
                    final long byteS = dis.readLong();
                    re = new RawEntry2() {

                        @Override ConstantPoolEntry
                        cook() {
                            return new ConstantLongInfo() { {
                                this.bytes = byteS;
                            } };
                        }
                    };
                    i += 2;
                    break;
                }
            case 6: // CONSTANT_Double_info
                {
                    final double byteS = dis.readDouble();
                    re = new RawEntry2() {

                        @Override ConstantPoolEntry
                        cook() {
                            return new ConstantDoubleInfo() { {
                                this.bytes = byteS;
                            } };
                        }
                    };
                    i += 2;
                    break;
                }
            case 12: // CONSTANT_NameAndType_info
                {
                    final short nameIndex       = dis.readShort();
                    final short descriptorIndex = dis.readShort();
                    re = new RawEntry2() {

                        @Override ConstantPoolEntry
                        cook() {
                            return new ConstantNameAndTypeInfo(
                                this.getConstantUtf8Info(nameIndex),
                                this.getConstantUtf8Info(descriptorIndex)
                            );
                        }
                    };
                    i++;
                    break;
                }
            case 1: // CONSTANT_Utf8_info
                {
                    final String bytes = dis.readUTF();
                    re = new RawEntry2() {

                        @Override ConstantPoolEntry
                        cook() {
                            return new ConstantUtf8Info(bytes);
                        }
                    };
                    i++;
                    break;
                }
            default:
                throw new RuntimeException("Invalid cp_info tag '" + (int) tag + "' on entry #" + i + " of " + count);
            }
            rawEntries[idx] = re;
        }

        this.entries = new ConstantPoolEntry[count];
        for (int i = 0; i < count; ++i) {
            try {
                if (this.entries[i] == null && rawEntries[i] != null) this.entries[i] = rawEntries[i].cook();
            } catch (RuntimeException re) {
                throw new RuntimeException("Cooking CP entry #" + i + " of " + count + ": " + re.getMessage(), re);
            }
        }
    }

    /** Checks that the indexed constant pool entry has the given {@code clasS}, and returns it. */
    public <T extends ConstantPoolEntry> T
    get(short index, Class<T> clasS) {
        int ii = 0xffff & index;
        if (ii == 0 || ii >= this.entries.length) {
            throw new IllegalArgumentException(
                "Illegal constant pool index " + ii + " - only 1..." + (this.entries.length - 1) + " allowed"
            );
        }

        ConstantPoolEntry e = this.entries[ii];
        if (e == null) throw new NullPointerException("Unusable CP entry " + index);
        if (!clasS.isAssignableFrom(e.getClass())) {
            throw new RuntimeException(
                "CP entry #" + index + " is a '" + e.getClass().getName() + "', not a '" + clasS.getName() + "'"
            );
        }

        @SuppressWarnings("unchecked") T result = (T) e;
        return result;
    }

    /** @return {@code null} iff {@code index == 0} */
    @Nullable public <T extends ConstantPoolEntry> T
    getOptional(short index, Class<T> clasS) {
        if (index == 0) return null;

        int ii = 0xffff & index;
        if (ii >= this.entries.length) {
            throw new IllegalArgumentException(
                "Illegal constant pool index " + ii + " - only 0..." + (this.entries.length - 1) + " allowed"
            );
        }

        ConstantPoolEntry e = this.entries[ii];
        if (e == null) throw new NullPointerException("Unusable CP entry " + index);
        if (!clasS.isAssignableFrom(e.getClass())) {
            throw new RuntimeException(
                "CP entry #" + index + " is a '" + e.getClass().getName() + "', not a '" + clasS.getName() + "'"
            );
        }

        @SuppressWarnings("unchecked") T result = (T) e;
        return result;
    }

    /**
     * Checks that the indexed constant pool entry is of type {@code CONSTANT_(Integer|Float|Class|String)_info}, and
     * returns its value converted to {@link String}.
     */
    public String
    getIntegerFloatClassString(short index) {
        ConstantPoolEntry e = this.get(index, ConstantPoolEntry.class);
        if (e instanceof ConstantIntegerInfo) return e.toString();
        if (e instanceof ConstantFloatInfo) return e.toString();
        if (e instanceof ConstantClassInfo) return e.toString();
        if (e instanceof ConstantStringInfo) return e.toString();
        throw new ClassCastException("CP index " + (0xffff & index) + ": " + e);
    }

    /**
     * Checks that the indexed constant pool entry is of type {@code CONSTANT_(Integer|Float|Long|Double|String)_info},
     * and returns its value converted to {@link String}.
     */
    public String
    getIntegerFloatLongDoubleString(short index) {
        ConstantPoolEntry e = this.get(index, ConstantPoolEntry.class);
        if (e instanceof ConstantIntegerInfo) return e.toString();
        if (e instanceof ConstantFloatInfo) return e.toString();
        if (e instanceof ConstantLongInfo) return e.toString();
        if (e instanceof ConstantDoubleInfo) return e.toString();
        if (e instanceof ConstantStringInfo) return ConstantPool.stringToJavaLiteral(((ConstantStringInfo) e).string);
        throw new ClassCastException("CP index " + (0xffff & index) + ": " + e);
    }

    /**
     * Checks that the indexed constant pool entry is of type {@code CONSTANT_(Long|Double|String)_info}, and returns
     * its value converted to {@link String}.
     */
    public String
    getLongDoubleString(short index) {
        ConstantPoolEntry e = this.get(index, ConstantPoolEntry.class);
        if (e instanceof ConstantLongInfo) return e.toString();
        if (e instanceof ConstantDoubleInfo) return e.toString();
        if (e instanceof ConstantStringInfo) return ConstantPool.stringToJavaLiteral(((ConstantStringInfo) e).string);
        throw new ClassCastException("CP index " + (0xffff & index) + ": " + e);
    }

    /**
     * Checks that the indexed constant pool entry is of type {@code CONSTANT_(Integer|Float|Long|Double)_info}, and
     * returns its value converted to {@link String}.
     */
    public String
    getIntegerFloatLongDouble(short index) {
        ConstantPoolEntry e = this.get(index, ConstantPoolEntry.class);
        if (e instanceof ConstantIntegerInfo) return e.toString();
        if (e instanceof ConstantFloatInfo) return e.toString();
        if (e instanceof ConstantLongInfo) return e.toString();
        if (e instanceof ConstantDoubleInfo) return e.toString();
        throw new ClassCastException("CP index " + (0xffff & index) + ": " + e);
    }

    /** @return The number of entries in this {@link >ConstantPool} */
    public int
    getSize() { return this.entries.length; }

    /**
     * Converts a given string into a Java literal by enclosing it in double quotes and escaping any special
     * characters.
     */
    public static String
    stringToJavaLiteral(String s) {
        for (int i = 0; i < s.length();) {
            char c   = s.charAt(i);
            int  idx = "\r\n\"\t\b".indexOf(c);
            if (idx == -1) {
                ++i;
            } else {
                s = s.substring(0, i) + '\\' + "rn\"tb".charAt(idx) + s.substring(i + 1);
                i += 2;
            }
            if (i >= 80) return '"' + s.substring(0, i) + "\"...";
        }
        return '"' + s + '"';
    }
}
