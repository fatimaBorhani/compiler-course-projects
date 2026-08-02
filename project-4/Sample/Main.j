.class public Main
.super BaseModule

.method public <init>()V
    .limit stack 128
    .limit locals 128
    aload_0
    invokespecial BaseModule/<init>()V
    return
.end method

.method public main()V
    .limit stack 128
    .limit locals 128
    ldc 5
    istore 1
    iload 1
    ldc 2
    imul
    istore 1
    aload_0
    iload 1
    invokevirtual Main/process(I)I
    istore 2
    new BaseModule
    dup
    invokespecial BaseModule/<init>()V
    astore 3
    aload 3
    iload 1
    invokevirtual BaseModule/process(I)I
    istore 4
    getstatic java/lang/System/out Ljava/io/PrintStream;
    iload 2
    invokevirtual java/io/PrintStream/println(I)V
    getstatic java/lang/System/out Ljava/io/PrintStream;
    iload 4
    invokevirtual java/io/PrintStream/println(I)V
    return
.end method

.method public static main([Ljava/lang/String;)V
    .limit stack 128
    .limit locals 128
    new Main
    dup
    invokespecial Main/<init>()V
    invokevirtual Main/main()V
    return
.end method