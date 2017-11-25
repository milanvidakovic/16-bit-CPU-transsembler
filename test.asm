	TITLE	stdio.c
	.386P
include listing.inc
if @Version gt 510
.model FLAT
else
_TEXT	SEGMENT PARA USE32 PUBLIC 'CODE'
_TEXT	ENDS
_DATA	SEGMENT DWORD USE32 PUBLIC 'DATA'
_DATA	ENDS
CONST	SEGMENT DWORD USE32 PUBLIC 'CONST'
CONST	ENDS
_BSS	SEGMENT DWORD USE32 PUBLIC 'BSS'
_BSS	ENDS
_TLS	SEGMENT DWORD USE32 PUBLIC 'TLS'
_TLS	ENDS
FLAT	GROUP _DATA, CONST, _BSS
	ASSUME	CS: FLAT, DS: FLAT, SS: FLAT
endif
PUBLIC	_print_str
_TEXT	SEGMENT
; File stdio.c
_s$ = 8
_v$ = -4
_i$ = -8
_print_str PROC NEAR
; Line 3
	push	ebp
	mov	ebp, esp
	sub	esp, 8
	push	ebx
	push	esi
	push	edi
; Line 4
	mov	DWORD PTR _v$[ebp], 65520		; 0000fff0H
; Line 7
	mov	DWORD PTR _i$[ebp], 0
	jmp	$L28
$L29:
	inc	DWORD PTR _i$[ebp]
$L28:
	cmp	DWORD PTR _i$[ebp], 16			; 00000010H
	jge	$L30
; Line 9
	mov	eax, DWORD PTR _s$[ebp]
	movsx	eax, BYTE PTR [eax]
	test	eax, eax
	jne	$L31
; Line 10
	jmp	$L30
; Line 11
$L31:
	mov	eax, DWORD PTR _s$[ebp]
	movsx	eax, BYTE PTR [eax]
	mov	ecx, DWORD PTR _v$[ebp]
	mov	DWORD PTR [ecx], eax
; Line 12
	add	DWORD PTR _v$[ebp], 4
; Line 13
	inc	DWORD PTR _s$[ebp]
; Line 14
	jmp	$L29
$L30:
; Line 15
$L25:
	pop	edi
	pop	esi
	pop	ebx
	leave
	ret	0
_print_str ENDP
_TEXT	ENDS
PUBLIC	_print_numz
_TEXT	SEGMENT
_n$ = 8
_i$ = -20
_d$ = -16
_s$ = -12
_print_numz PROC NEAR
; Line 18
	push	ebp
	mov	ebp, esp
	sub	esp, 20					; 00000014H
	push	ebx
	push	esi
	push	edi
; Line 21
	mov	BYTE PTR _s$[ebp], 48			; 00000030H
	mov	BYTE PTR _s$[ebp+1], 48			; 00000030H
	mov	BYTE PTR _s$[ebp+2], 48			; 00000030H
	mov	BYTE PTR _s$[ebp+3], 48			; 00000030H
	mov	BYTE PTR _s$[ebp+4], 48			; 00000030H
	mov	BYTE PTR _s$[ebp+5], 48			; 00000030H
	mov	BYTE PTR _s$[ebp+6], 0
	mov	BYTE PTR _s$[ebp+7], 0
	mov	BYTE PTR _s$[ebp+8], 0
	mov	BYTE PTR _s$[ebp+9], 0
; Line 23
	mov	DWORD PTR _i$[ebp], 0
	jmp	$L38
$L39:
	inc	DWORD PTR _i$[ebp]
$L38:
	cmp	DWORD PTR _i$[ebp], 5
	jge	$L40
; Line 24
	mov	ecx, 10					; 0000000aH
	mov	eax, DWORD PTR _n$[ebp]
	cdq
	idiv	ecx
	mov	DWORD PTR _d$[ebp], edx
; Line 25
	mov	ecx, 10					; 0000000aH
	mov	eax, DWORD PTR _n$[ebp]
	cdq
	idiv	ecx
	mov	DWORD PTR _n$[ebp], eax
; Line 26
	mov	eax, 5
	sub	eax, DWORD PTR _i$[ebp]
	movsx	eax, BYTE PTR _s$[ebp+eax]
	add	eax, DWORD PTR _d$[ebp]
	mov	ecx, 5
	sub	ecx, DWORD PTR _i$[ebp]
	mov	BYTE PTR _s$[ebp+ecx], al
; Line 27
	cmp	DWORD PTR _n$[ebp], 0
	jne	$L41
; Line 28
	jmp	$L40
; Line 29
$L41:
	jmp	$L39
$L40:
; Line 30
	lea	eax, DWORD PTR _s$[ebp]
	push	eax
	call	_print_str
	add	esp, 4
; Line 31
$L34:
	pop	edi
	pop	esi
	pop	ebx
	leave
	ret	0
_print_numz ENDP
_TEXT	ENDS
PUBLIC	_print_num
_TEXT	SEGMENT
_n$ = 8
_i$ = -32
_d$ = -28
_s$ = -24
_print_num PROC NEAR
; Line 34
	push	ebp
	mov	ebp, esp
	sub	esp, 36					; 00000024H
	push	ebx
	push	esi
	push	edi
; Line 40
	mov	DWORD PTR _i$[ebp], 0
	jmp	$L50
$L51:
	inc	DWORD PTR _i$[ebp]
$L50:
	cmp	DWORD PTR _i$[ebp], 5
	jge	$L52
; Line 42
	mov	ecx, 10					; 0000000aH
	mov	eax, DWORD PTR _n$[ebp]
	cdq
	idiv	ecx
	mov	DWORD PTR _d$[ebp], edx
; Line 43
	mov	ecx, 10					; 0000000aH
	mov	eax, DWORD PTR _n$[ebp]
	cdq
	idiv	ecx
	mov	DWORD PTR _n$[ebp], eax
; Line 44
	mov	eax, DWORD PTR _d$[ebp]
	add	eax, 48					; 00000030H
	mov	ecx, DWORD PTR _i$[ebp]
	mov	BYTE PTR _s$[ebp+ecx], al
; Line 45
	cmp	DWORD PTR _n$[ebp], 0
	jne	$L53
; Line 46
	jmp	$L52
; Line 47
$L53:
	jmp	$L51
$L52:
; Line 48
	mov	eax, DWORD PTR _i$[ebp]
	mov	BYTE PTR _s$[ebp+eax+1], 0
; Line 49
	lea	eax, DWORD PTR _s$[ebp]
	push	eax
	call	_print_str
	add	esp, 4
; Line 50
$L44:
	pop	edi
	pop	esi
	pop	ebx
	leave
	ret	0
_print_num ENDP
_TEXT	ENDS
PUBLIC	_strlen
_TEXT	SEGMENT
_c$ = 8
_res$ = -4
_strlen	PROC NEAR
; Line 53
	push	ebp
	mov	ebp, esp
	sub	esp, 8
	push	ebx
	push	esi
	push	edi
; Line 54
	mov	DWORD PTR _res$[ebp], 0
; Line 55
$L59:
	mov	eax, DWORD PTR _c$[ebp]
	mov	DWORD PTR -8+[ebp], eax
	inc	DWORD PTR _c$[ebp]
	mov	eax, DWORD PTR -8+[ebp]
	movsx	eax, BYTE PTR [eax]
	test	eax, eax
	je	$L60
; Line 57
	inc	DWORD PTR _res$[ebp]
; Line 58
	jmp	$L59
$L60:
; Line 59
	mov	eax, DWORD PTR _res$[ebp]
	jmp	$L56
; Line 60
$L56:
	pop	edi
	pop	esi
	pop	ebx
	leave
	ret	0
_strlen	ENDP
_TEXT	ENDS
END
