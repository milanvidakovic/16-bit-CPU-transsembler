_strlen:
push b
ld b, sp
add sp, 5

; Line 54
ld a, 0
st a, [b+1]
; Line 55
_strlenL59:
ld a, [b-3]
push a
ld a, 1
add a, [b-3]
st a, [b-3]
pop a
push c
ld c, a
ld a, [c]
pop c
cmp a, 0
jz _strlenL60
; Line 57
ld a, 1
add a, [b+1]
st a, [b+1]
; Line 58
jmp _strlenL59
_strlenL60:
; Line 59
ld a, [b+1]
jmp _strlenL56
; Line 60
_strlenL56:

ld sp, b
pop b
ret

_print_numz:
push b
ld b, sp
add sp, 21

; Line 21
ld a, 48
st a, [b+9]
ld a, 48
st a, [b+10]
ld a, 48
st a, [b+11]
ld a, 48
st a, [b+12]
ld a, 48
st a, [b+13]
ld a, 48
st a, [b+14]
ld a, 0
st a, [b+15]
ld a, 0
st a, [b+16]
ld a, 0
st a, [b+17]
ld a, 0
st a, [b+18]
; Line 23
ld a, 0
st a, [b+1]
jmp _print_numzL38
_print_numzL39:
ld a, 1
add a, [b+1]
st a, [b+1]
_print_numzL38:
ld a, [b+1]
cmp a, 5
jp _print_numzL40
; Line 24
ld c, 10
ld a, [b-3]
div a, c
ld a, h
st a, [b+5]
; Line 25
ld c, 10
ld a, [b-3]
div a, c
st a, [b-3]
; Line 26
ld a, 5
sub a, [b +1]
add a, b
ld c, a
ld a, [c+9]
add a, [b+5]
ld c, 5
push a
ld a, c
sub a, [b +1]
ld c, a
pop a
st a, [b+c+9]
; Line 27
ld a, [b-3]
cmp a, 0
jnz _print_numzL41
; Line 28
jmp _print_numzL40
; Line 29
_print_numzL41:
jmp _print_numzL39
_print_numzL40:
; Line 30
ld a, 9
add a, b
push a
call _print_str
sub sp, 1
; Line 31
_print_numzL34:

ld sp, b
pop b
ret

_print_str:
push b
ld b, sp
add sp, 9

; Line 4
ld a, 65520
st a, [b+5]
; Line 7
ld a, 0
st a, [b+1]
jmp _print_strL28
_print_strL29:
ld a, 1
add a, [b+1]
st a, [b+1]
_print_strL28:
ld a, [b+1]
cmp a, 16
jp _print_strL30
; Line 9
ld a, [b-3]
push c
ld c, a
ld a, [c]
pop c
cmp a, 0
jnz _print_strL31
; Line 10
jmp _print_strL30
; Line 11
_print_strL31:
ld a, [b-3]
push c
ld c, a
ld a, [c]
pop c
push a
ld a, [b+5]
ld c, a
pop a
st a, [c]
; Line 12
ld a, [b+5]
add a, 1
st a, [b+5]
; Line 13
ld a, 1
add a, [b-3]
st a, [b-3]
; Line 14
jmp _print_strL29
_print_strL30:
; Line 15
_print_strL25:

ld sp, b
pop b
ret

_print_num:
push b
ld b, sp
add sp, 33

; Line 40
ld a, 0
st a, [b+1]
jmp _print_numL50
_print_numL51:
ld a, 1
add a, [b+1]
st a, [b+1]
_print_numL50:
ld a, [b+1]
cmp a, 5
jp _print_numL52
; Line 42
ld c, 10
ld a, [b-3]
div a, c
ld a, h
st a, [b+5]
; Line 43
ld c, 10
ld a, [b-3]
div a, c
st a, [b-3]
; Line 44
ld a, [b+5]
add a,48
push a
ld a, [b+1]
ld c, a
pop a
st a, [b+c+9]
; Line 45
ld a, [b-3]
cmp a, 0
jnz _print_numL53
; Line 46
jmp _print_numL52
; Line 47
_print_numL53:
jmp _print_numL51
_print_numL52:
; Line 48
ld a, [b+1]
add a, b
inc a
ld c, a
ld a, 0
st a, [c+10]
; Line 49
ld a, 9
add a, b
push a
call _print_str
sub sp, 1
; Line 50
_print_numL44:

ld sp, b
pop b
ret

