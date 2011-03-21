
/*
 * JDISASM - A Java[TM] class file disassembler
 *
 * Copyright (c) 2001-2011, Arno Unkrig
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

/**
 * Representation of the "constant pool" in a Java&trade; class file.
 */
public class ConstantPool {

    /** Representation of a constant pool entry. */
    public interface ConstantPoolEntry {
    }

    /** Representation of a CONSTANT_Class_info entry. */
    public static class ConstantClassInfo implements ConstantPoolEntry {
        public String name;
    }

    /** Representation of a CONSTANT_Fieldref_info entry. */
    public static class ConstantFieldrefInfo implements ConstantPoolEntry {
        public ConstantClassInfo       clasS;
        public ConstantNameAndTypeInfo nameAndType;
    }

    /** Representation of a CONSTANT_Methodref_info entry. */
    public static class ConstantMethodrefInfo implements ConstantPoolEntry {
        public ConstantClassInfo       clasS;
        public ConstantNameAndTypeInfo nameAndType;
    }

    /** Representation of a CONSTANT_InterfaceMethodref_info entry. */
    public static class ConstantInterfaceMethodrefInfo implements ConstantPoolEntry {
        public ConstantClassInfo       clasS;
        public ConstantNameAndTypeInfo nameAndType;
    }

    /** Representation of a CONSTANT_String_info entry. */
    public static class ConstantStringInfo implements ConstantPoolEntry {
        public String string;
    }

    /** Representation of a CONSTANT_Integer_info entry. */
    public static class ConstantIntegerInfo implements ConstantPoolEntry {
        public int bytes;
    }

    /** Representation of a CONSTANT_Float_info entry. */
    public static class ConstantFloatInfo implements ConstantPoolEntry {
        public float bytes;
    }

    /** Representation of a CONSTANT_Long_info entry. */
    public static class ConstantLongInfo implements ConstantPoolEntry {
        public long bytes;
    }

    /** Representation of a CONSTANT_Double_info entry. */
    public static class ConstantDoubleInfo implements ConstantPoolEntry {
        public double bytes;
    }

    /** Representation of a CONSTANT_NameAndType_info entry. */
    public static class ConstantNameAndTypeInfo implements ConstantPoolEntry {
        public ConstantUtf8Info name;
        public ConstantUtf8Info descriptor;
    }

    /** Representation of a CONSTANT_Utf8_info entry. */
    public static class ConstantUtf8Info implements ConstantPoolEntry {
        public String bytes;
    }

    private final ConstantPoolEntry[] entries;

    /**
     * Reads a constant pool from the given {@link InputStream}. Afterwards, entries can be retrieved by invoking
     * the {@code getConstant*Info()} method family.
     *
     * @throws ClassCastException             An entry has the "wrong" type
     * @throws NullPointerException           An "unusable" entry (the magic "zero" entry and the entries after a LONG
     *                                        or DOUBLE entry) is referenced
     * @throws ArrayIndexOutOfBoundsException An index is too small or to great
     */
    public ConstantPool(DataInputStream dis) throws IOException {
        short count = dis.readShort();

        // Read the entries into a temporary data structure - this is necessary because there may be forward
        // references.
        /***/
        abstract class RawEntry {
            abstract ConstantPoolEntry cook();
            ConstantClassInfo getConstantClassInfo(short index) {
                return (ConstantClassInfo) get(index);
            }
            ConstantNameAndTypeInfo getConstantNameAndTypeInfo(short index) {
                return (ConstantNameAndTypeInfo) get(index);
            }
            ConstantUtf8Info getConstantUtf8Info(short index) {
                return (ConstantUtf8Info) get(index);
            }
            abstract ConstantPoolEntry get(short index);
        }
        final RawEntry[] rawEntries = new RawEntry[0xffff & count];

        /***/
        abstract class RawEntry2 extends RawEntry {
            ConstantPoolEntry get(short index) {
                if (ConstantPool.this.entries[0xffff & index] == null) {
                    ConstantPool.this.entries[0xffff & index] = new ConstantPoolEntry() { }; // To prevent recursion.
                    ConstantPool.this.entries[0xffff & index] = rawEntries[0xffff & index].cook();
                }
                return ConstantPool.this.entries[0xffff & index];
            }
        }

        for (short i = 1; i != count;) {
            int idx = 0xffff & i;
            RawEntry re;
            byte tag = dis.readByte();
            switch (tag) {
            case 7: // CONSTANT_Class_info
                {
                    final short nameIndex = dis.readShort();
                    re = new RawEntry2() {
                        ConstantPoolEntry cook() {
                            return new ConstantClassInfo() { {
                                this.name = getConstantUtf8Info(nameIndex).bytes.replace('/', '.');
                            } };
                        }
                    };
                    i++;
                    break;
                }
            case 9: // CONSTANT_Fieldref_info
                {
                    final short classIndex = dis.readShort();
                    final short nameAndTypeIndex = dis.readShort();
                    re = new RawEntry2() {
                        ConstantPoolEntry cook() {
                            return new ConstantFieldrefInfo() { {
                                this.clasS = getConstantClassInfo(classIndex);
                                this.nameAndType = getConstantNameAndTypeInfo(nameAndTypeIndex);
                            } };
                        }
                    };
                    i++;
                    break;
                }
            case 10: // CONSTANT_Methodref_info
                {
                    final short classIndex = dis.readShort();
                    final short nameAndTypeIndex = dis.readShort();
                    re = new RawEntry2() {
                        ConstantPoolEntry cook() {
                            return new ConstantMethodrefInfo() { {
                                this.clasS = getConstantClassInfo(classIndex);
                                this.nameAndType = getConstantNameAndTypeInfo(nameAndTypeIndex);
                            } };
                        }
                    };
                    i++;
                    break;
                }
            case 11: // CONSTANT_InterfaceMethodref_info
                {
                    final short classIndex = dis.readShort();
                    final short nameAndTypeIndex = dis.readShort();
                    re = new RawEntry2() {
                        ConstantPoolEntry cook() {
                            return new ConstantInterfaceMethodrefInfo() { {
                                this.clasS = getConstantClassInfo(classIndex);
                                this.nameAndType = getConstantNameAndTypeInfo(nameAndTypeIndex);
                            } };
                        }
                    };
                    i++;
                    break;
                }
            case 8: // CONSTANT_String_info
                {
                    final short stringIndex = dis.readShort();
                    re = new RawEntry2() {
                        ConstantPoolEntry cook() {
                            return new ConstantStringInfo() { {
                                this.string = getConstantUtf8Info(stringIndex).bytes;
                            } };
                        }
                    };
                    i++;
                    break;
                }
            case 3: // CONSTANT_Integer_info
                {
                    final int byteS = dis.readInt();
                    re = new RawEntry2() {
                        ConstantPoolEntry cook() {
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
                        ConstantPoolEntry cook() {
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
                        ConstantPoolEntry cook() {
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
                        ConstantPoolEntry cook() {
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
                    final short nameIndex = dis.readShort();
                    final short descriptorIndex = dis.readShort();
                    re = new RawEntry2() {
                        ConstantPoolEntry cook() {
                            return new ConstantNameAndTypeInfo() { {
                                this.name = getConstantUtf8Info(nameIndex);
                                this.descriptor = getConstantUtf8Info(descriptorIndex);
                            } };
                        }
                    };
                    i++;
                    break;
                }
            case 1: // CONSTANT_Utf8_info
                {
                    final String byteS = dis.readUTF();
                    re = new RawEntry2() {
                        ConstantPoolEntry cook() {
                            return new ConstantUtf8Info() { {
                                this.bytes = byteS;
                            } };
                        }
                    };
                    i++;
                    break;
                }
            default:
                throw new RuntimeException("Invalid cp_info tag \"" + (int) tag + "\"");
            }
            rawEntries[idx] = re;
        }

        this.entries = new ConstantPoolEntry[0xffff & count];
        for (int i = 0; i < (0xffff & count); ++i) {
            if (this.entries[i] == null && rawEntries[i] != null) this.entries[i] = rawEntries[i].cook();
        }
    }

    private ConstantPoolEntry get(short index) {
        if (index == 0) return null;
        ConstantPoolEntry e = this.entries[0xffff & index];
        if (e == null) throw new NullPointerException("Unusable CP entry " + index);
        return e;
    }

    /* CHECKSTYLE LineLength:OFF */
    public ConstantClassInfo              getConstantClassInfo(short index)              { return (ConstantClassInfo)              get(index); }
    public ConstantFieldrefInfo           getConstantFieldrefInfo(short index)           { return (ConstantFieldrefInfo)           get(index); }
    public ConstantMethodrefInfo          getConstantMethodrefInfo(short index)          { return (ConstantMethodrefInfo)          get(index); }
    public ConstantInterfaceMethodrefInfo getConstantInterfaceMethodrefInfo(short index) { return (ConstantInterfaceMethodrefInfo) get(index); }
    public ConstantStringInfo             getConstantStringInfo(short index)             { return (ConstantStringInfo)             get(index); }
    public ConstantIntegerInfo            getConstantIntegerInfo(short index)            { return (ConstantIntegerInfo)            get(index); }
    public ConstantFloatInfo              getConstantFloatInfo(short index)              { return (ConstantFloatInfo)              get(index); }
    public ConstantLongInfo               getConstantLongInfo(short index)               { return (ConstantLongInfo)               get(index); }
    public ConstantDoubleInfo             getConstantDoubleInfo(short index)             { return (ConstantDoubleInfo)             get(index); }
    public ConstantNameAndTypeInfo        getConstantNameAndTypeInfo(short index)        { return (ConstantNameAndTypeInfo)        get(index); }
    public ConstantUtf8Info               getConstantUtf8Info(short index)               { return (ConstantUtf8Info)               get(index); }
    /* CHECKSTYLE LineLength:ON */

    /**
     * Checks that the indexed constant pool entry is of type {@code CONSTANT_(Integer|Float|Class|String)_info}, and
     * returns its value converted to {@link String}.
     */
    public String getIntegerFloatClassString(short index) {
        ConstantPoolEntry e = this.entries[index];
        if (e instanceof ConstantIntegerInfo) return Integer.toString(((ConstantIntegerInfo) e).bytes);
        if (e instanceof ConstantFloatInfo) return Float.toString(((ConstantFloatInfo) e).bytes);
        if (e instanceof ConstantClassInfo) return ((ConstantClassInfo) e).name;
        if (e instanceof ConstantStringInfo) return stringToJavaLiteral(((ConstantStringInfo) e).string);
        throw new ClassCastException(e.getClass().getName());
    }

    /**
     * Checks that the indexed constant pool entry is of type {@code CONSTANT_(Integer|Float|Long|Double|String)_info},
     * and returns its value converted to {@link String}.
     */
    public String getIntegerFloatLongDoubleString(short index) {
        ConstantPoolEntry e = this.entries[index];
        if (e instanceof ConstantIntegerInfo) return Integer.toString(((ConstantIntegerInfo) e).bytes);
        if (e instanceof ConstantFloatInfo) return Float.toString(((ConstantFloatInfo) e).bytes);
        if (e instanceof ConstantLongInfo) return Long.toString(((ConstantLongInfo) e).bytes);
        if (e instanceof ConstantDoubleInfo) return Double.toString(((ConstantDoubleInfo) e).bytes);
        if (e instanceof ConstantStringInfo) return stringToJavaLiteral(((ConstantStringInfo) e).string);
        throw new ClassCastException(e.getClass().getName());
    }
    
    /**
     * Checks that the indexed constant pool entry is of type {@code CONSTANT_(Long|Double|String)_info}, and returns
     * its value converted to {@link String}.
     */
    public String getLongDoubleString(short index) {
        ConstantPoolEntry e = this.entries[index];
        if (e instanceof ConstantLongInfo) return Long.toString(((ConstantLongInfo) e).bytes) + 'L';
        if (e instanceof ConstantDoubleInfo) return Double.toString(((ConstantDoubleInfo) e).bytes) + 'D';
        throw new ClassCastException(e.getClass().getName());
    }
    
    /**
     * Checks that the indexed constant pool entry is of type {@code CONSTANT_(Integer|Float|Long|Double)_info}, and
     * returns its value converted to {@link String}.
     */
    public String getIntegerFloatLongDouble(short index) {
        ConstantPoolEntry e = this.entries[index];
        if (e instanceof ConstantIntegerInfo) return Integer.toString(((ConstantIntegerInfo) e).bytes);
        if (e instanceof ConstantFloatInfo) return Float.toString(((ConstantFloatInfo) e).bytes);
        if (e instanceof ConstantLongInfo) return Long.toString(((ConstantLongInfo) e).bytes);
        if (e instanceof ConstantDoubleInfo) return Double.toString(((ConstantDoubleInfo) e).bytes);
        throw new ClassCastException(e.getClass().getName());
    }

    public static String stringToJavaLiteral(String s) {
        for (int i = 0; i < s.length();) {
            char c = s.charAt(i);
            int idx = "\r\n\"\t\b".indexOf(c);
            if (idx == -1) {
                ++i;
            } else {
                s = s.substring(0, i) + '\\' + "rn\"tb".charAt(idx) + s.substring(i + 1);
                i += 2;
            }
            if (i >= 80) break;
        }
        return '"' + s + '"';
    }
}
