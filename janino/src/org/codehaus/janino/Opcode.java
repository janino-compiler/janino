
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2006, Arno Unkrig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *    1. Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *    2. Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *    3. The name of the author may not be used to endorse or promote
 *       products derived from this software without specific prior
 *       written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.codehaus.janino;

/**
 * Definitions of Java bytecode opcodes.
 */

/*package*/ interface Opcode {
    public final static byte AALOAD = 50;
    public final static byte AASTORE = 83;
    public final static byte ACONST_NULL = 1;
    public final static byte ALOAD = 25;
    public final static byte ALOAD_0 = 42;
    public final static byte ALOAD_1 = 43;
    public final static byte ALOAD_2 = 44;
    public final static byte ALOAD_3 = 45;
    public final static byte ANEWARRAY = (byte) 189;
    public final static byte ARETURN = (byte) 176;
    public final static byte ARRAYLENGTH = (byte) 190;
    public final static byte ASTORE = 58;
    public final static byte ASTORE_0 = 75;
    public final static byte ASTORE_1 = 76;
    public final static byte ASTORE_2 = 77;
    public final static byte ASTORE_3 = 78;
    public final static byte ATHROW = (byte) 191;
    public final static byte BALOAD = 51;
    public final static byte BASTORE = 84;
    public final static byte BIPUSH = 16;
    public final static byte CALOAD = 52;
    public final static byte CASTORE = 85;
    public final static byte CHECKCAST = (byte) 192;
    public final static byte D2F = (byte) 144;
    public final static byte D2I = (byte) 142;
    public final static byte D2L = (byte) 143;
    public final static byte DADD = 99;
    public final static byte DALOAD = 49;
    public final static byte DASTORE = 82;
    public final static byte DCMPG = (byte) 152;
    public final static byte DCMPL = (byte) 151;
    public final static byte DCONST_0 = 14;
    public final static byte DCONST_1 = 15;
    public final static byte DDIV = 111;
    public final static byte DLOAD = 24;
    public final static byte DLOAD_0 = 38;
    public final static byte DLOAD_1 = 39;
    public final static byte DLOAD_2 = 40;
    public final static byte DLOAD_3 = 41;
    public final static byte DMUL = 107;
    public final static byte DNEG = 119;
    public final static byte DREM = 115;
    public final static byte DRETURN = (byte) 175;
    public final static byte DSTORE = 57;
    public final static byte DSTORE_0 = 71;
    public final static byte DSTORE_1 = 72;
    public final static byte DSTORE_2 = 73;
    public final static byte DSTORE_3 = 74;
    public final static byte DSUB = 103;
    public final static byte DUP = 89;
    public final static byte DUP_X1 = 90;
    public final static byte DUP_X2 = 91;
    public final static byte DUP2 = 92;
    public final static byte DUP2_X1 = 93;
    public final static byte DUP2_X2 = 94;
    public final static byte F2D = (byte) 141;
    public final static byte F2I = (byte) 139;
    public final static byte F2L = (byte) 140;
    public final static byte FADD = 98;
    public final static byte FALOAD = 48;
    public final static byte FASTORE = 81;
    public final static byte FCMPG = (byte) 150;
    public final static byte FCMPL = (byte) 149;
    public final static byte FCONST_0 = 11;
    public final static byte FCONST_1 = 12;
    public final static byte FCONST_2 = 13;
    public final static byte FDIV = 110;
    public final static byte FLOAD = 23;
    public final static byte FLOAD_0 = 34;
    public final static byte FLOAD_1 = 35;
    public final static byte FLOAD_2 = 36;
    public final static byte FLOAD_3 = 37;
    public final static byte FMUL = 106;
    public final static byte FNEG = 118;
    public final static byte FREM = 114;
    public final static byte FRETURN = (byte) 174;
    public final static byte FSTORE = 56;
    public final static byte FSTORE_0 = 67;
    public final static byte FSTORE_1 = 68;
    public final static byte FSTORE_2 = 69;
    public final static byte FSTORE_3 = 70;
    public final static byte FSUB = 102;
    public final static byte GETFIELD = (byte) 180;
    public final static byte GETSTATIC = (byte) 178;
    public final static byte GOTO = (byte) 167;
    public final static byte GOTO_W = (byte) 200;
    public final static byte I2B = (byte) 145;
    public final static byte I2C = (byte) 146;
    public final static byte I2D = (byte) 135;
    public final static byte I2F = (byte) 134;
    public final static byte I2L = (byte) 133;
    public final static byte I2S = (byte) 147;
    public final static byte IADD = 96;
    public final static byte IALOAD = 46;
    public final static byte IAND = 126;
    public final static byte IASTORE = 79;
    public final static byte ICONST_M1 = 2;
    public final static byte ICONST_0 = 3;
    public final static byte ICONST_1 = 4;
    public final static byte ICONST_2 = 5;
    public final static byte ICONST_3 = 6;
    public final static byte ICONST_4 = 7;
    public final static byte ICONST_5 = 8;
    public final static byte IDIV = 108;
    public final static byte IF_ACMPEQ = (byte) 165;
    public final static byte IF_ACMPNE = (byte) 166;
    public final static byte IF_ICMPEQ = (byte) 159;
    public final static byte IF_ICMPNE = (byte) 160;
    public final static byte IF_ICMPLT = (byte) 161;
    public final static byte IF_ICMPGE = (byte) 162;
    public final static byte IF_ICMPGT = (byte) 163;
    public final static byte IF_ICMPLE = (byte) 164;
    public final static byte IFEQ = (byte) 153;
    public final static byte IFNE = (byte) 154;
    public final static byte IFLT = (byte) 155;
    public final static byte IFGE = (byte) 156;
    public final static byte IFGT = (byte) 157;
    public final static byte IFLE = (byte) 158;
    public final static byte IFNONNULL = (byte) 199;
    public final static byte IFNULL = (byte) 198;
    public final static byte IINC = (byte) 132;
    public final static byte ILOAD = 21;
    public final static byte ILOAD_0 = 26;
    public final static byte ILOAD_1 = 27;
    public final static byte ILOAD_2 = 28;
    public final static byte ILOAD_3 = 29;
    public final static byte IMUL = 104;
    public final static byte INEG = 116;
    public final static byte INSTANCEOF = (byte) 193;
    public final static byte INVOKEINTERFACE = (byte) 185;
    public final static byte INVOKESPECIAL = (byte) 183;
    public final static byte INVOKESTATIC = (byte) 184;
    public final static byte INVOKEVIRTUAL = (byte) 182;
    public final static byte IOR = (byte) 128;
    public final static byte IREM = 112;
    public final static byte IRETURN = (byte) 172;
    public final static byte ISHL = 120;
    public final static byte ISHR = 122;
    public final static byte ISTORE = 54;
    public final static byte ISTORE_0 = 59;
    public final static byte ISTORE_1 = 60;
    public final static byte ISTORE_2 = 61;
    public final static byte ISTORE_3 = 62;
    public final static byte ISUB = 100;
    public final static byte IUSHR = 124;
    public final static byte IXOR = (byte) 130;
    public final static byte JSR = (byte) 168;
    public final static byte JSR_W = (byte) 201;
    public final static byte L2D = (byte) 138;
    public final static byte L2F = (byte) 137;
    public final static byte L2I = (byte) 136;
    public final static byte LADD = 97;
    public final static byte LALOAD = 47;
    public final static byte LAND = 127;
    public final static byte LASTORE = 80;
    public final static byte LCMP = (byte) 148;
    public final static byte LCONST_0 = 9;
    public final static byte LCONST_1 = 10;
    public final static byte LDC = 18;
    public final static byte LDC_W = 19;
    public final static byte LDC2_W = 20;
    public final static byte LDIV = 109;
    public final static byte LLOAD = 22;
    public final static byte LLOAD_0 = 30;
    public final static byte LLOAD_1 = 31;
    public final static byte LLOAD_2 = 32;
    public final static byte LLOAD_3 = 33;
    public final static byte LMUL = 105;
    public final static byte LNEG = 117;
    public final static byte LOOKUPSWITCH = (byte) 171;
    public final static byte LOR = (byte) 129;
    public final static byte LREM = 113;
    public final static byte LRETURN = (byte) 173;
    public final static byte LSHL = 121;
    public final static byte LSHR = 123;
    public final static byte LSTORE = 55;
    public final static byte LSTORE_0 = 63;
    public final static byte LSTORE_1 = 64;
    public final static byte LSTORE_2 = 65;
    public final static byte LSTORE_3 = 66;
    public final static byte LSUB = 101;
    public final static byte LUSHR = 125;
    public final static byte LXOR = (byte) 131;
    public final static byte MONITORENTER = (byte) 194;
    public final static byte MONITOREXIT = (byte) 195;
    public final static byte MULTIANEWARRAY = (byte) 197;
    public final static byte NEW = (byte) 187;
    public final static byte NEWARRAY = (byte) 188;
    public final static byte NOP = 0;
    public final static byte POP = 87;
    public final static byte POP2 = 88;
    public final static byte PUTFIELD = (byte) 181;
    public final static byte PUTSTATIC = (byte) 179;
    public final static byte RET = (byte) 169;
    public final static byte RETURN = (byte) 177;
    public final static byte SALOAD = 53;
    public final static byte SASTORE = 86;
    public final static byte SIPUSH = 17;
    public final static byte SWAP = 95;
    public final static byte TABLESWITCH = (byte) 170;
    public final static byte WIDE = (byte) 196;

    /**
     * Constants for the "OPCODE_PROPERTIES" array.
     */
    public static final short INVALID_OPCODE = -1;

    // "Stack delta" constants.
    public static final short SD_MASK            = 31;
    public static final short SD_M4              = 0;
    public static final short SD_M3              = 1;
    public static final short SD_M2              = 2;
    public static final short SD_M1              = 3;
    public static final short SD_P0              = 4;
    public static final short SD_P1              = 5;
    public static final short SD_P2              = 6;
    public static final short SD_0               = 7;
    public static final short SD_GETFIELD        = 9;
    public static final short SD_GETSTATIC       = 10;
    public static final short SD_PUTFIELD        = 11;
    public static final short SD_PUTSTATIC       = 12;
    public static final short SD_INVOKEVIRTUAL   = 13;
    public static final short SD_INVOKESPECIAL   = 14;
    public static final short SD_INVOKESTATIC    = 15;
    public static final short SD_INVOKEINTERFACE = 16;
    public static final short SD_MULTIANEWARRAY  = 18;

    // Operand 1 types.
    public static final short OP1_MASK         = 15 * 32;
    public static final short OP1_SB           =  1 * 32;
    public static final short OP1_UB           =  2 * 32;
    public static final short OP1_SS           =  3 * 32;
    public static final short OP1_CP1          =  4 * 32;
    public static final short OP1_CP2          =  5 * 32;
    public static final short OP1_LV1          =  6 * 32;
    public static final short OP1_LV2          =  7 * 32;
    public static final short OP1_BO2          =  8 * 32;
    public static final short OP1_BO4          =  9 * 32;
    public static final short OP1_LOOKUPSWITCH = 10 * 32;
    public static final short OP1_TABLESWITCH  = 11 * 32;
    public static final short OP1_JSR          = 12 * 32;

    // Operand 2 types.
    public static final short OP2_MASK = 3 * 512;
    public static final short OP2_SB   = 1 * 512;
    public static final short OP2_SS   = 2 * 512;

    // Operand 3 types.
    public static final short OP3_MASK = 1 * 2048;
    public static final short OP3_SB   = 1 * 2048;

    // Implicit operands.
    public static final short IO_MASK = 7 * 4096;
    public static final short IO_LV_0 = 1 * 4096;
    public static final short IO_LV_1 = 2 * 4096;
    public static final short IO_LV_2 = 3 * 4096;
    public static final short IO_LV_3 = 4 * 4096;

    public static final short NO_FALLTHROUGH = (short) 32768;

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
/*200*/ /*GOTO_W*/          Opcode.SD_P0 | Opcode.OP1_BO4,
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

    public static final short[] WIDE_OPCODE_PROPERTIES = {
/*  0*/ Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
        Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
        Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
/*010*/ Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
        Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
        Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
/*020*/ Opcode.INVALID_OPCODE,
        /*ILOAD*/ Opcode.SD_P1 | Opcode.OP1_LV2,
        /*LLOAD*/ Opcode.SD_P2 | Opcode.OP1_LV2,
        /*FLOAD*/ Opcode.SD_P1 | Opcode.OP1_LV2,
        /*DLOAD*/ Opcode.SD_P2 | Opcode.OP1_LV2,
        /*ALOAD*/ Opcode.SD_P1 | Opcode.OP1_LV2,
        Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
/* 30*/ Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
        Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
        Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
/* 40*/ Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
        Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
        Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
/* 50*/ Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
        /*ISTORE*/ Opcode.SD_M1 | Opcode.OP1_LV2,
        /*LSTORE*/ Opcode.SD_M2 | Opcode.OP1_LV2,
        /*FSTORE*/ Opcode.SD_M1 | Opcode.OP1_LV2,
        /*DSTORE*/ Opcode.SD_M2 | Opcode.OP1_LV2,
        /*ASTORE*/ Opcode.SD_M1 | Opcode.OP1_LV2,
        Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
/* 60*/ Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
        Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
        Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
/* 70*/ Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
        Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
        Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
/* 80*/ Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
        Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
        Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
/* 90*/ Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
        Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
        Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
/*100*/ Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
        Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
        Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
/*110*/ Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
        Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
        Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
/*120*/ Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
        Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
        Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
/*130*/ Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
        /*IINC*/ Opcode.SD_P0 | Opcode.OP1_LV2 | Opcode.OP2_SS,
        Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
        Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
/*140*/ Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
        Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
        Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
/*150*/ Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
        Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
        Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
/*160*/ Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
        Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
        Opcode.INVALID_OPCODE,
        /*RET*/ Opcode.SD_P0 | Opcode.OP1_LV2,
/*170*/ Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
        Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
        Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
/*180*/ Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
        Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
        Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
/*190*/ Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
        Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
        Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
/*200*/ Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
        Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
        Opcode.INVALID_OPCODE, Opcode.INVALID_OPCODE,
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
}
