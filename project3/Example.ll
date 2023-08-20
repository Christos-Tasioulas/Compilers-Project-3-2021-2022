target triple = "x86_64-pc-linux-gnu"
declare i32 @printf(i8*, ...)
declare i8* @calloc(i32, i32)
@.str = constant [4 x i8] c"%d\0A\00"

define void @main() {
	%a =  alloca i32
	%b =  alloca i32
	%i =  alloca i32
	%j =  alloca i32
	%c =  alloca i8
	%d =  alloca i8
	%e =  alloca i8
	%f =  alloca i32*
	%g =  alloca i32
	%h =  alloca i32
	%k =  alloca i8
	store i32 5, i32* %a
	store i32 2, i32* %b
	%1 = load i32, i32* %a
	%2 = load i32, i32* %b
	%3 = add i32 %1, %2
	store i32 %3, i32* %a
	%4 = load i32, i32* %a
	%5 = load i32, i32* %b
	%6 = add i32 %4, %5
	%7 = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @.str, i64 0, i64 0), i32 %6)
	store i32 3, i32* %i
	store i32 5, i32* %j
	%8 = load i32, i32* %i
	%9 = load i32, i32* %j
	%10 = icmp slt i32 %8, %9
	%11 = zext i1 %10 to i8
	store i8 %11, i8* %c
	%12 = load i8, i8* %c
	%13 = trunc i8 %12 to i1
	%14 = xor i1 %13, 1
	%15 = zext i1 %14 to i8
	store i8 %15, i8* %d
	%16 = load i8, i8* %d
	%17 = trunc i8 %16 to i1
	br i1 %17, label %18, label %21

18:
	%19 = load i8, i8* %c
	%20 = trunc i8 %19 to i1
	br label %21

21:
	%22 = phi i1 [ false, %0 ], [%20, %18]
	%23 = zext i1 %22 to i8
	store i8 %23, i8* %e
	%24 = load i32, i32* %i
	%25 = load i32, i32* %j
	%26 = add i32 %24, %25
	%27 = load i32, i32* %b
	%28 = add i32 %27, %26
	store i32 %28, i32* %a
	%29 = load i32, i32* %a
	%30 = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @.str, i64 0, i64 0), i32 %29)
	store i32 5, i32* %a
	%31 = call i8* @calloc(i32 5, i32 4)
	%32 = bitcast i8* %31 to i32*
	store i32* %32, i32** %f
	%33 = load i32*, i32** %f
	%34 = getelementptr inbounds i32, i32* %33, i32 3
	store i32 5, i32* %34
	store i8 1, i8* %k
	%35 = load i8, i8* %k
	%36 = trunc i8 %35 to i1
	br i1 %36, label %if34, label %else34

if34:
	store i8 0, i8* %k
	br label %continue34

else34:
	store i8 1, i8* %k
	br label %continue34

continue34:
	br label %while36

while36:
	%37 = load i8, i8* %k
	%38 = trunc i8 %37 to i1
	br i1 %38, label %loop36, label %break36

loop36:
	store i8 0, i8* %k
	br label %while36

break36:
	ret void
}
