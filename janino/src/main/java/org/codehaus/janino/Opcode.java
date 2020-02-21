
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2010 Arno Unkrig. All rights reserved.
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

/**
 * Definitions of JVM bytecode opcodes.
 */
final
class Opcode {
    private Opcode() {}

    // Symbolic JVM opcodes, in alphabetical order.

    // SUPPRESS CHECKSTYLE JavadocVariable:216
    public static final int AALOAD          = 50;
    public static final int AASTORE         = 83;
    public static final int ACONST_NULL     = 1;
    public static final int ALOAD           = 25;
    public static final int ALOAD_0         = 42;
    public static final int ALOAD_1         = 43;
    public static final int ALOAD_2         = 44;
    public static final int ALOAD_3         = 45;
    public static final int ANEWARRAY       = 189;
    public static final int ARETURN         = 176;
    public static final int ARRAYLENGTH     = 190;
    public static final int ASTORE          = 58;
    public static final int ASTORE_0        = 75;
    public static final int ASTORE_1        = 76;
    public static final int ASTORE_2        = 77;
    public static final int ASTORE_3        = 78;
    public static final int ATHROW          = 191;
    public static final int BALOAD          = 51;
    public static final int BASTORE         = 84;
    public static final int BIPUSH          = 16;
    public static final int CALOAD          = 52;
    public static final int CASTORE         = 85;
    public static final int CHECKCAST       = 192;
    public static final int D2F             = 144;
    public static final int D2I             = 142;
    public static final int D2L             = 143;
    public static final int DADD            = 99;
    public static final int DALOAD          = 49;
    public static final int DASTORE         = 82;
    public static final int DCMPG           = 152;
    public static final int DCMPL           = 151;
    public static final int DCONST_0        = 14;
    public static final int DCONST_1        = 15;
    public static final int DDIV            = 111;
    public static final int DLOAD           = 24;
    public static final int DLOAD_0         = 38;
    public static final int DLOAD_1         = 39;
    public static final int DLOAD_2         = 40;
    public static final int DLOAD_3         = 41;
    public static final int DMUL            = 107;
    public static final int DNEG            = 119;
    public static final int DREM            = 115;
    public static final int DRETURN         = 175;
    public static final int DSTORE          = 57;
    public static final int DSTORE_0        = 71;
    public static final int DSTORE_1        = 72;
    public static final int DSTORE_2        = 73;
    public static final int DSTORE_3        = 74;
    public static final int DSUB            = 103;
    public static final int DUP             = 89;
    public static final int DUP_X1          = 90;
    public static final int DUP_X2          = 91;
    public static final int DUP2            = 92;
    public static final int DUP2_X1         = 93;
    public static final int DUP2_X2         = 94;
    public static final int F2D             = 141;
    public static final int F2I             = 139;
    public static final int F2L             = 140;
    public static final int FADD            = 98;
    public static final int FALOAD          = 48;
    public static final int FASTORE         = 81;
    public static final int FCMPG           = 150;
    public static final int FCMPL           = 149;
    public static final int FCONST_0        = 11;
    public static final int FCONST_1        = 12;
    public static final int FCONST_2        = 13;
    public static final int FDIV            = 110;
    public static final int FLOAD           = 23;
    public static final int FLOAD_0         = 34;
    public static final int FLOAD_1         = 35;
    public static final int FLOAD_2         = 36;
    public static final int FLOAD_3         = 37;
    public static final int FMUL            = 106;
    public static final int FNEG            = 118;
    public static final int FREM            = 114;
    public static final int FRETURN         = 174;
    public static final int FSTORE          = 56;
    public static final int FSTORE_0        = 67;
    public static final int FSTORE_1        = 68;
    public static final int FSTORE_2        = 69;
    public static final int FSTORE_3        = 70;
    public static final int FSUB            = 102;
    public static final int GETFIELD        = 180;
    public static final int GETSTATIC       = 178;
    public static final int GOTO            = 167;
    public static final int GOTO_W          = 200;
    public static final int I2B             = 145;
    public static final int I2C             = 146;
    public static final int I2D             = 135;
    public static final int I2F             = 134;
    public static final int I2L             = 133;
    public static final int I2S             = 147;
    public static final int IADD            = 96;

    /**
     * @see <a href="https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-6.html#jvms-6.5.iaload">JVMS 8</a>
     */
    public static final int IALOAD          = 46;
    public static final int IAND            = 126;
    public static final int IASTORE         = 79;
    public static final int ICONST_M1       = 2;
    public static final int ICONST_0        = 3;
    public static final int ICONST_1        = 4;
    public static final int ICONST_2        = 5;
    public static final int ICONST_3        = 6;
    public static final int ICONST_4        = 7;
    public static final int ICONST_5        = 8;
    public static final int IDIV            = 108;
    public static final int IF_ACMPEQ       = 165;
    public static final int IF_ACMPNE       = 166;
    public static final int IF_ICMPEQ       = 159;
    public static final int IF_ICMPNE       = 160;
    public static final int IF_ICMPLT       = 161;
    public static final int IF_ICMPGE       = 162;
    public static final int IF_ICMPGT       = 163;
    public static final int IF_ICMPLE       = 164;
    public static final int IFEQ            = 153;
    public static final int IFNE            = 154;
    public static final int IFLT            = 155;
    public static final int IFGE            = 156;
    public static final int IFGT            = 157;
    public static final int IFLE            = 158;
    public static final int IFNONNULL       = 199;
    public static final int IFNULL          = 198;
    public static final int IINC            = 132;
    public static final int ILOAD           = 21;
    public static final int ILOAD_0         = 26;
    public static final int ILOAD_1         = 27;
    public static final int ILOAD_2         = 28;
    public static final int ILOAD_3         = 29;
    public static final int IMUL            = 104;
    public static final int INEG            = 116;
    public static final int INSTANCEOF      = 193;
    public static final int INVOKEDYNAMIC   = 186;
    public static final int INVOKEINTERFACE = 185;
    public static final int INVOKESPECIAL   = 183;
    public static final int INVOKESTATIC    = 184;
    public static final int INVOKEVIRTUAL   = 182;
    public static final int IOR             = 128;
    public static final int IREM            = 112;
    public static final int IRETURN         = 172;
    public static final int ISHL            = 120;
    public static final int ISHR            = 122;
    public static final int ISTORE          = 54;
    public static final int ISTORE_0        = 59;
    public static final int ISTORE_1        = 60;
    public static final int ISTORE_2        = 61;
    public static final int ISTORE_3        = 62;
    public static final int ISUB            = 100;
    public static final int IUSHR           = 124;
    public static final int IXOR            = 130;

    /**
     * @deprecated Only allowed until .class file version 50.0 (Java 6) JVMS 4.10.2.5
     */
    @Deprecated public static final int JSR = 168;

    public static final int JSR_W           = 201;
    public static final int L2D             = 138;
    public static final int L2F             = 137;
    public static final int L2I             = 136;
    public static final int LADD            = 97;
    public static final int LALOAD          = 47;
    public static final int LAND            = 127;
    public static final int LASTORE         = 80;
    public static final int LCMP            = 148;
    public static final int LCONST_0        = 9;
    public static final int LCONST_1        = 10;
    public static final int LDC             = 18;
    public static final int LDC_W           = 19;
    public static final int LDC2_W          = 20;
    public static final int LDIV            = 109;
    public static final int LLOAD           = 22;
    public static final int LLOAD_0         = 30;
    public static final int LLOAD_1         = 31;
    public static final int LLOAD_2         = 32;
    public static final int LLOAD_3         = 33;
    public static final int LMUL            = 105;
    public static final int LNEG            = 117;
    public static final int LOOKUPSWITCH    = 171;
    public static final int LOR             = 129;
    public static final int LREM            = 113;
    public static final int LRETURN         = 173;
    public static final int LSHL            = 121;
    public static final int LSHR            = 123;
    public static final int LSTORE          = 55;
    public static final int LSTORE_0        = 63;
    public static final int LSTORE_1        = 64;
    public static final int LSTORE_2        = 65;
    public static final int LSTORE_3        = 66;
    public static final int LSUB            = 101;
    public static final int LUSHR           = 125;
    public static final int LXOR            = 131;
    public static final int MONITORENTER    = 194;
    public static final int MONITOREXIT     = 195;
    public static final int MULTIANEWARRAY  = 197;
    public static final int NEW             = 187;
    public static final int NEWARRAY        = 188;
    public static final int NOP             = 0;
    public static final int POP             = 87;
    public static final int POP2            = 88;
    public static final int PUTFIELD        = 181;
    public static final int PUTSTATIC       = 179;

    /**
     * @deprecated Only allowed until .class file version 50.0 (Java 6) JVMS 4.10.2.5
     */
    @Deprecated public static final int RET = 169;

    public static final int RETURN          = 177;
    public static final int SALOAD          = 53;
    public static final int SASTORE         = 86;
    public static final int SIPUSH          = 17;
    public static final int SWAP            = 95;
    public static final int TABLESWITCH     = 170;
    public static final int WIDE            = 196;

    // Constants for the "OPCODE_PROPERTIES" array.

    /**
     * Special value for {@link #OPCODE_PROPERTIES} indicating that this element represents an invalid opcode.
     */
    public static final short INVALID_OPCODE = -1;

    /**
     * Masks the 'stack delta' portion of {@link #OPCODE_PROPERTIES}.
     */
    public static final short SD_MASK = 31;

    /**
     * Indicates that the opcode represented by this element of {@link #OPCODE_PROPERTIES} reduces the operand stack
     * size by 4 elements.
     */
    public static final short SD_M4 = 0;

    /**
     * Indicates that the opcode represented by this element of {@link #OPCODE_PROPERTIES} reduces the operand stack
     * size by 3 elements.
     */
    public static final short SD_M3 = 1;

    /**
     * Indicates that the opcode represented by this element of {@link #OPCODE_PROPERTIES} reduces the operand stack
     * size by 2 elements.
     */
    public static final short SD_M2 = 2;

    /**
     * Indicates that the opcode represented by this element of {@link #OPCODE_PROPERTIES} reduces the operand stack
     * size by 1 element.
     */
    public static final short SD_M1 = 3;

    /**
     * Indicates that the opcode represented by this element of {@link #OPCODE_PROPERTIES} results in the same operand
     * stack size.
     */
    public static final short SD_P0 = 4;

    /**
     * Indicates that the opcode represented by this element of {@link #OPCODE_PROPERTIES} increases the operand stack
     * size by 1 element.
     */
    public static final short SD_P1 = 5;

    /**
     * Indicates that the opcode represented by this element of {@link #OPCODE_PROPERTIES} increases the operand stack
     * size by 2 elements.
     */
    public static final short SD_P2 = 6;

    /**
     * Indicates that the opcode represented by this element of {@link #OPCODE_PROPERTIES} clears the operand stack.
     */
    public static final short SD_0 = 7;

    /**
     * This element of {@link #OPCODE_PROPERTIES} represents the GETFIELD opcode.
     */
    public static final short SD_GETFIELD = 9;

    /**
     * This element of {@link #OPCODE_PROPERTIES} represents the GETSTATIC opcode.
     */
    public static final short SD_GETSTATIC = 10;

    /**
     * This element of {@link #OPCODE_PROPERTIES} represents the PUTFIELD opcode.
     */
    public static final short SD_PUTFIELD = 11;

    /**
     * This element of {@link #OPCODE_PROPERTIES} represents the PUTSTATIC opcode.
     */
    public static final short SD_PUTSTATIC = 12;

    /**
     * This element of {@link #OPCODE_PROPERTIES} represents the INVOKEVIRTUAL opcode.
     */
    public static final short SD_INVOKEVIRTUAL = 13;

    /**
     * This element of {@link #OPCODE_PROPERTIES} represents the INVOKESPECIAL opcode.
     */
    public static final short SD_INVOKESPECIAL = 14;

    /**
     * This element of {@link #OPCODE_PROPERTIES} represents the INVOKESTATIC opcode.
     */
    public static final short SD_INVOKESTATIC = 15;

    /**
     * This element of {@link #OPCODE_PROPERTIES} represents the INVOKEINTERFACE opcode.
     */
    public static final short SD_INVOKEINTERFACE = 16;

    /**
     * This element of {@link #OPCODE_PROPERTIES} represents the MULTIANEWARRAY opcode.
     */
    public static final short SD_MULTIANEWARRAY = 18;

    // Properties of the opcode's first operand.

    /**
     * Masks the 'first operand' portion of {@link #OPCODE_PROPERTIES}.
     */
    public static final short OP1_MASK = 15 * 32;

    /**
     * The first operand of this opcode is a signed byte.
     */
    public static final short OP1_SB =  1 * 32;

    /**
     * The first operand of this opcode is an unsigned byte.
     */
    public static final short OP1_UB =  2 * 32;

    /**
     * The first operand of this opcode is a signed short.
     */
    public static final short OP1_SS =  3 * 32;

    /**
     * The first operand of this opcode is a one-byte constant pool index.
     */
    public static final short OP1_CP1 =  4 * 32;

    /**
     * The first operand of this opcode is a two-byte constant pool index.
     */
    public static final short OP1_CP2 =  5 * 32;

    /**
     * The first operand of this opcode is a one-byte local variable array index.
     */
    public static final short OP1_LV1 =  6 * 32;

    /**
     * The first operand of this opcode is a two-byte local variable array index.
     */
    public static final short OP1_LV2 =  7 * 32;

    /**
     * The first operand of this opcode is a two-byte branch offset.
     */
    public static final short OP1_BO2 =  8 * 32;

    /**
     * The first operand of this opcode is a four-byte branch offset.
     */
    public static final short OP1_BO4 =  9 * 32;

    /**
     * The first operand of this opcode is a signed byte.
     */
    public static final short OP1_LOOKUPSWITCH = 10 * 32;

    /**
     * The first operand of this opcode is a signed byte.
     */
    public static final short OP1_TABLESWITCH = 11 * 32;

    /**
     * The first operand of this opcode is a signed byte.
     */
    public static final short OP1_JSR = 12 * 32;

    // Properties of the opcode's second operand.

    /**
     * Masks the 'second operand' portion of {@link #OPCODE_PROPERTIES}.
     */
    public static final short OP2_MASK = 3 * 512;

    /**
     * The second operand of this opcode is a signed byte.
     */
    public static final short OP2_SB = 1 * 512;

    /**
     * The second operand of this opcode is a signed short.
     */
    public static final short OP2_SS = 2 * 512;

    // Properties of the opcode's third operand.

    /**
     * Masks the 'third operand' portion of {@link #OPCODE_PROPERTIES}.
     */
    public static final short OP3_MASK = 1 * 2048;

    /**
     * The third operand of this opcode is a signed byte.
     */
    public static final short OP3_SB = 1 * 2048;

    // Properties of the opcode's 'implicit' operand.

    /**
     * Masks the 'implicit operand' portion of {@link #OPCODE_PROPERTIES}.
     */
    public static final short IO_MASK = 7 * 4096;

    /**
     * The local variable with index 0 is the opcode's implicit operand.
     */
    public static final short IO_LV_0 = 1 * 4096;

    /**
     * The local variable with index 1 is the opcode's implicit operand.
     */
    public static final short IO_LV_1 = 2 * 4096;

    /**
     * The local variable with index 2 is the opcode's implicit operand.
     */
    public static final short IO_LV_2 = 3 * 4096;

    /**
     * The local variable with index 3 is the opcode's implicit operand.
     */
    public static final short IO_LV_3 = 4 * 4096;

    /**
     * This opcode never 'completes normally', i.e. it never passes the control flow to the immediately following
     * opcode.
     */
    public static final short NO_FALLTHROUGH = (short) 32768;

    /**
     * The <var>n</var>th element of this array describes the properties of the JVM opcode <var>n</var>.
     */
    public static final short[] OPCODE_PROPERTIES = {
/*  0*/ /*NOP*/             Opcode.SD_P0,
        /*ACONST_NULL*/     Opcode.SD_P1,
        /*ICONST_M1*/       Opcode.SD_P1,
        /*ICONST_0*/        Opcode.SD_P1,
        /*ICONST_1*/        Opcode.SD_P1,
        /*ICONST_2*/        Opcode.SD_P1,
        /*ICONST_3*/        Opcode.SD_P1,
        /*ICONST_4*/        Opcode.SD_P1,
        /*ICONST_5*/        Opcode.SD_P1,
        /*LCONST_0*/        Opcode.SD_P2,
/* 10*/ /*LCONST_1*/        Opcode.SD_P2,
        /*FCONST_0*/        Opcode.SD_P1,
        /*FCONST_1*/        Opcode.SD_P1,
        /*FCONST_2*/        Opcode.SD_P1,
        /*DCONST_0*/        Opcode.SD_P2,
        /*DCONST_1*/        Opcode.SD_P2,
        /*BIPUSH*/          Opcode.SD_P1 | Opcode.OP1_SB,
        /*SIPUSH*/          Opcode.SD_P1 | Opcode.OP1_SS,
        /*LDC*/             Opcode.SD_P1 | Opcode.OP1_CP1,
        /*LDC_W*/           Opcode.SD_P1 | Opcode.OP1_CP2,
/* 20*/ /*LDC2_W*/          Opcode.SD_P2 | Opcode.OP1_CP2,
        /*ILOAD*/           Opcode.SD_P1 | Opcode.OP1_LV1,
        /*LLOAD*/           Opcode.SD_P2 | Opcode.OP1_LV1,
        /*FLOAD*/           Opcode.SD_P1 | Opcode.OP1_LV1,
        /*DLOAD*/           Opcode.SD_P2 | Opcode.OP1_LV1,
        /*ALOAD*/           Opcode.SD_P1 | Opcode.OP1_LV1,
        /*ILOAD_0*/         Opcode.SD_P1 | Opcode.IO_LV_0,
        /*ILOAD_1*/         Opcode.SD_P1 | Opcode.IO_LV_1,
        /*ILOAD_2*/         Opcode.SD_P1 | Opcode.IO_LV_2,
        /*ILOAD_3*/         Opcode.SD_P1 | Opcode.IO_LV_3,
/* 30*/ /*LLOAD_0*/         Opcode.SD_P2 | Opcode.IO_LV_0,
        /*LLOAD_1*/         Opcode.SD_P2 | Opcode.IO_LV_1,
        /*LLOAD_2*/         Opcode.SD_P2 | Opcode.IO_LV_2,
        /*LLOAD_3*/         Opcode.SD_P2 | Opcode.IO_LV_3,
        /*FLOAD_0*/         Opcode.SD_P1 | Opcode.IO_LV_0,
        /*FLOAD_1*/         Opcode.SD_P1 | Opcode.IO_LV_1,
        /*FLOAD_2*/         Opcode.SD_P1 | Opcode.IO_LV_2,
        /*FLOAD_3*/         Opcode.SD_P1 | Opcode.IO_LV_3,
        /*DLOAD_0*/         Opcode.SD_P2 | Opcode.IO_LV_0,
        /*DLOAD_1*/         Opcode.SD_P2 | Opcode.IO_LV_1,
/* 40*/ /*DLOAD_2*/         Opcode.SD_P2 | Opcode.IO_LV_2,
        /*DLOAD_3*/         Opcode.SD_P2 | Opcode.IO_LV_3,
        /*ALOAD_0*/         Opcode.SD_P1 | Opcode.IO_LV_0,
        /*ALOAD_1*/         Opcode.SD_P1 | Opcode.IO_LV_1,
        /*ALOAD_2*/         Opcode.SD_P1 | Opcode.IO_LV_2,
        /*ALOAD_3*/         Opcode.SD_P1 | Opcode.IO_LV_3,
        /*IALOAD*/          Opcode.SD_M1,
        /*LALOAD*/          Opcode.SD_P0,
        /*FALOAD*/          Opcode.SD_M1,
        /*DALOAD*/          Opcode.SD_P0,
/* 50*/ /*AALOAD*/          Opcode.SD_M1,
        /*BALOAD*/          Opcode.SD_M1,
        /*CALOAD*/          Opcode.SD_M1,
        /*SALOAD*/          Opcode.SD_M1,
        /*ISTORE*/          Opcode.SD_M1 | Opcode.OP1_LV1,
        /*LSTORE*/          Opcode.SD_M2 | Opcode.OP1_LV1,
        /*FSTORE*/          Opcode.SD_M1 | Opcode.OP1_LV1,
        /*DSTORE*/          Opcode.SD_M2 | Opcode.OP1_LV1,
        /*ASTORE*/          Opcode.SD_M1 | Opcode.OP1_LV1,
        /*ISTORE_0*/        Opcode.SD_M1 | Opcode.IO_LV_0,
/* 60*/ /*ISTORE_1*/        Opcode.SD_M1 | Opcode.IO_LV_1,
        /*ISTORE_2*/        Opcode.SD_M1 | Opcode.IO_LV_2,
        /*ISTORE_3*/        Opcode.SD_M1 | Opcode.IO_LV_3,
        /*LSTORE_0*/        Opcode.SD_M2 | Opcode.IO_LV_0,
        /*LSTORE_1*/        Opcode.SD_M2 | Opcode.IO_LV_1,
        /*LSTORE_2*/        Opcode.SD_M2 | Opcode.IO_LV_2,
        /*LSTORE_3*/        Opcode.SD_M2 | Opcode.IO_LV_3,
        /*FSTORE_0*/        Opcode.SD_M1 | Opcode.IO_LV_0,
        /*FSTORE_1*/        Opcode.SD_M1 | Opcode.IO_LV_1,
        /*FSTORE_2*/        Opcode.SD_M1 | Opcode.IO_LV_2,
/* 70*/ /*FSTORE_3*/        Opcode.SD_M1 | Opcode.IO_LV_3,
        /*DSTORE_0*/        Opcode.SD_M2 | Opcode.IO_LV_0,
        /*DSTORE_1*/        Opcode.SD_M2 | Opcode.IO_LV_1,
        /*DSTORE_2*/        Opcode.SD_M2 | Opcode.IO_LV_2,
        /*DSTORE_3*/        Opcode.SD_M2 | Opcode.IO_LV_3,
        /*ASTORE_0*/        Opcode.SD_M1 | Opcode.IO_LV_0,
        /*ASTORE_1*/        Opcode.SD_M1 | Opcode.IO_LV_1,
        /*ASTORE_2*/        Opcode.SD_M1 | Opcode.IO_LV_2,
        /*ASTORE_3*/        Opcode.SD_M1 | Opcode.IO_LV_3,
        /*IASTORE*/         Opcode.SD_M3,
/* 80*/ /*LASTORE*/         Opcode.SD_M4,
        /*FASTORE*/         Opcode.SD_M3,
        /*DASTORE*/         Opcode.SD_M4,
        /*AASTORE*/         Opcode.SD_M3,
        /*BASTORE*/         Opcode.SD_M3,
        /*CASTORE*/         Opcode.SD_M3,
        /*SASTORE*/         Opcode.SD_M3,
        /*POP*/             Opcode.SD_M1,
        /*POP2*/            Opcode.SD_M2,
        /*DUP*/             Opcode.SD_P1,
/* 90*/ /*DUP_X1*/          Opcode.SD_P1,
        /*DUP_X2*/          Opcode.SD_P1,
        /*DUP2*/            Opcode.SD_P2,
        /*DUP2_X1*/         Opcode.SD_P2,
        /*DUP2_X2*/         Opcode.SD_P2,
        /*SWAP*/            Opcode.SD_P0,
        /*IADD*/            Opcode.SD_M1,
        /*LADD*/            Opcode.SD_M2,
        /*FADD*/            Opcode.SD_M1,
        /*DADD*/            Opcode.SD_M2,
/*100*/ /*ISUB*/            Opcode.SD_M1,
        /*LSUB*/            Opcode.SD_M2,
        /*FSUB*/            Opcode.SD_M1,
        /*DSUB*/            Opcode.SD_M2,
        /*IMUL*/            Opcode.SD_M1,
        /*LMUL*/            Opcode.SD_M2,
        /*FMUL*/            Opcode.SD_M1,
        /*DMUL*/            Opcode.SD_M2,
        /*IDIV*/            Opcode.SD_M1,
        /*LDIV*/            Opcode.SD_M2,
/*110*/ /*FDIV*/            Opcode.SD_M1,
        /*DDIV*/            Opcode.SD_M2,
        /*IREM*/            Opcode.SD_M1,
        /*LREM*/            Opcode.SD_M2,
        /*FREM*/            Opcode.SD_M1,
        /*DREM*/            Opcode.SD_M2,
        /*INEG*/            Opcode.SD_P0,
        /*LNEG*/            Opcode.SD_P0,
        /*FNEG*/            Opcode.SD_P0,
        /*DNEG*/            Opcode.SD_P0,
/*120*/ /*ISHL*/            Opcode.SD_M1,
        /*LSHL*/            Opcode.SD_M1,
        /*ISHR*/            Opcode.SD_M1,
        /*LSHR*/            Opcode.SD_M1,
        /*IUSHR*/           Opcode.SD_M1,
        /*LUSHR*/           Opcode.SD_M1,
        /*IAND*/            Opcode.SD_M1,
        /*LAND*/            Opcode.SD_M2,
        /*IOR*/             Opcode.SD_M1,
        /*LOR*/             Opcode.SD_M2,
/*130*/ /*IXOR*/            Opcode.SD_M1,
        /*LXOR*/            Opcode.SD_M2,
        /*IINC*/            Opcode.SD_P0 | Opcode.OP1_LV1 | Opcode.OP2_SB,
        /*I2L*/             Opcode.SD_P1,
        /*I2F*/             Opcode.SD_P0,
        /*I2D*/             Opcode.SD_P1,
        /*L2I*/             Opcode.SD_M1,
        /*L2F*/             Opcode.SD_M1,
        /*L2D*/             Opcode.SD_P0,
        /*F2I*/             Opcode.SD_P0,
/*140*/ /*F2L*/             Opcode.SD_P1,
        /*F2D*/             Opcode.SD_P1,
        /*D2I*/             Opcode.SD_M1,
        /*D2L*/             Opcode.SD_P0,
        /*D2F*/             Opcode.SD_M1,
        /*I2B*/             Opcode.SD_P0,
        /*I2C*/             Opcode.SD_P0,
        /*I2S*/             Opcode.SD_P0,
        /*LCMP*/            Opcode.SD_M3,
        /*FCMPL*/           Opcode.SD_M1,
/*150*/ /*FCMPG*/           Opcode.SD_M1,
        /*DCMPL*/           Opcode.SD_M3,
        /*DCMPG*/           Opcode.SD_M3,
        /*IFEQ*/            Opcode.SD_M1 | Opcode.OP1_BO2,
        /*IFNE*/            Opcode.SD_M1 | Opcode.OP1_BO2,
        /*IFLT*/            Opcode.SD_M1 | Opcode.OP1_BO2,
        /*IFGE*/            Opcode.SD_M1 | Opcode.OP1_BO2,
        /*IFGT*/            Opcode.SD_M1 | Opcode.OP1_BO2,
        /*IFLE*/            Opcode.SD_M1 | Opcode.OP1_BO2,
        /*IF_ICMPEQ*/       Opcode.SD_M2 | Opcode.OP1_BO2,
/*160*/ /*IF_ICMPNE*/       Opcode.SD_M2 | Opcode.OP1_BO2,
        /*IF_ICMPLT*/       Opcode.SD_M2 | Opcode.OP1_BO2,
        /*IF_ICMPGE*/       Opcode.SD_M2 | Opcode.OP1_BO2,
        /*IF_ICMPGT*/       Opcode.SD_M2 | Opcode.OP1_BO2,
        /*IF_ICMPLE*/       Opcode.SD_M2 | Opcode.OP1_BO2,
        /*IF_ACMPEQ*/       Opcode.SD_M2 | Opcode.OP1_BO2,
        /*IF_ACMPNE*/       Opcode.SD_M2 | Opcode.OP1_BO2,
        /*GOTO*/            Opcode.SD_P0 | Opcode.OP1_BO2 | Opcode.NO_FALLTHROUGH,
        /*JSR*/             Opcode.SD_P0 | Opcode.OP1_JSR,
        /*RET*/             Opcode.SD_P0 | Opcode.OP1_LV1 | Opcode.NO_FALLTHROUGH,
/*170*/ /*TABLESWITCH*/     Opcode.SD_M1 | Opcode.OP1_TABLESWITCH,
        /*LOOKUPSWITCH*/    Opcode.SD_M1 | Opcode.OP1_LOOKUPSWITCH,
        /*IRETURN*/         Opcode.SD_0 | Opcode.NO_FALLTHROUGH,
        /*LRETURN*/         Opcode.SD_0 | Opcode.NO_FALLTHROUGH,
        /*FRETURN*/         Opcode.SD_0 | Opcode.NO_FALLTHROUGH,
        /*DRETURN*/         Opcode.SD_0 | Opcode.NO_FALLTHROUGH,
        /*ARETURN*/         Opcode.SD_M1 | Opcode.NO_FALLTHROUGH,
        /*RETURN*/          Opcode.SD_0 | Opcode.NO_FALLTHROUGH,
        /*GETSTATIC*/       Opcode.SD_GETSTATIC | Opcode.OP1_CP2,
        /*PUTSTATIC*/       Opcode.SD_PUTSTATIC | Opcode.OP1_CP2,
/*180*/ /*GETFIELD*/        Opcode.SD_GETFIELD | Opcode.OP1_CP2,
        /*PUTFIELD*/        Opcode.SD_PUTFIELD | Opcode.OP1_CP2,
        /*INVOKEVIRTUAL*/   Opcode.SD_INVOKEVIRTUAL | Opcode.OP1_CP2,
        /*INVOKESPECIAL*/   Opcode.SD_INVOKESPECIAL | Opcode.OP1_CP2,
        /*INVOKESTATIC*/    Opcode.SD_INVOKESTATIC | Opcode.OP1_CP2,
        /*INVOKEINTERFACE*/ Opcode.SD_INVOKEINTERFACE | Opcode.OP1_CP2 | Opcode.OP2_SB | Opcode.OP3_SB,
        /*UNUSED*/          Opcode.INVALID_OPCODE,
        /*NEW*/             Opcode.SD_P1 | Opcode.OP1_CP2,
        /*NEWARRAY*/        Opcode.SD_P0 | Opcode.OP1_UB,
        /*ANEWARRAY*/       Opcode.SD_P0 | Opcode.OP1_CP2,
/*190*/ /*ARRAYLENGTH*/     Opcode.SD_P0,
        /*ATHROW*/          Opcode.SD_M1 | Opcode.NO_FALLTHROUGH,
        /*CHECKCAST*/       Opcode.SD_P0 | Opcode.OP1_CP2,
        /*INSTANCEOF*/      Opcode.SD_P0 | Opcode.OP1_CP2,
        /*MONITORENTER*/    Opcode.SD_M1,
        /*MONITOREXIT*/     Opcode.SD_M1,
        /*WIDE*/            Opcode.INVALID_OPCODE,
        /*MULTIANEWARRAY*/  Opcode.SD_MULTIANEWARRAY | Opcode.OP1_CP2 | Opcode.OP2_SB,
        /*IFNULL*/          Opcode.SD_M1 | Opcode.OP1_BO2,
        /*IFNONNULL*/       Opcode.SD_M1 | Opcode.OP1_BO2,
/*200*/ /*GOTO_W*/          Opcode.SD_P0 | Opcode.OP1_BO4 | Opcode.NO_FALLTHROUGH,
        /*JSR_W*/           Opcode.SD_P1 | Opcode.OP1_BO4,
        Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
        Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
/*210*/ Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
        Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
        Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
/*220*/ Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
        Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
        Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
/*230*/ Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
        Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
        Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
/*240*/ Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
        Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
        Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
/*250*/ Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
        Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
    };

    /**
     * The <var>n</var>th element of this array describes the properties of the JVM opcode {@code WIDE n}.
     */
    public static final short[] WIDE_OPCODE_PROPERTIES = new short[256];
    static {
        for (int i = 0; i < Opcode.WIDE_OPCODE_PROPERTIES.length; ++i) {
            Opcode.WIDE_OPCODE_PROPERTIES[i] = Opcode.INVALID_OPCODE;
        }
        // load instructions
        Opcode.WIDE_OPCODE_PROPERTIES[0xff & Opcode.ILOAD] = Opcode.SD_P1 | Opcode.OP1_LV2;
        Opcode.WIDE_OPCODE_PROPERTIES[0xff & Opcode.FLOAD] = Opcode.SD_P1 | Opcode.OP1_LV2;
        Opcode.WIDE_OPCODE_PROPERTIES[0xff & Opcode.ALOAD] = Opcode.SD_P1 | Opcode.OP1_LV2;
        Opcode.WIDE_OPCODE_PROPERTIES[0xff & Opcode.LLOAD] = Opcode.SD_P2 | Opcode.OP1_LV2;
        Opcode.WIDE_OPCODE_PROPERTIES[0xff & Opcode.DLOAD] = Opcode.SD_P2 | Opcode.OP1_LV2;

        // store instructions
        Opcode.WIDE_OPCODE_PROPERTIES[0xff & Opcode.ISTORE] = Opcode.SD_M1 | Opcode.OP1_LV2;
        Opcode.WIDE_OPCODE_PROPERTIES[0xff & Opcode.FSTORE] = Opcode.SD_M1 | Opcode.OP1_LV2;
        Opcode.WIDE_OPCODE_PROPERTIES[0xff & Opcode.ASTORE] = Opcode.SD_M1 | Opcode.OP1_LV2;
        Opcode.WIDE_OPCODE_PROPERTIES[0xff & Opcode.LSTORE] = Opcode.SD_M2 | Opcode.OP1_LV2;
        Opcode.WIDE_OPCODE_PROPERTIES[0xff & Opcode.DSTORE] = Opcode.SD_M2 | Opcode.OP1_LV2;

        Opcode.WIDE_OPCODE_PROPERTIES[0xff & Opcode.IINC] = Opcode.SD_P0 | Opcode.OP1_LV2 | Opcode.OP2_SS;
        Opcode.WIDE_OPCODE_PROPERTIES[0xff & Opcode.RET]  = Opcode.SD_P0 | Opcode.OP1_LV2;
    }
}
