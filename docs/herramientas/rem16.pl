#!/usr/bin/perl
# Recalibra rem de una raíz de 12px a una raíz de 16px (1rem = 16px).
#   font-size:  x rem (12·x px)  ->  (12·x + 1)/16 rem   (la escala tipográfica sube 1px)
#   resto:      x rem            ->  0.75·x rem          (mismo tamaño en pantalla)
# Uso: perl rem16.pl css|xhtml < entrada > salida   (STDERR: conteos)
# Marcadores internos: \x02 (unidad ya convertida). Nunca '@' (Perl lo interpola).
use strict; use warnings;
my $modo = shift // 'css';
local $/; my $c = <STDIN>;
my ($nf, $nr) = (0, 0);

sub fmt { my $v = shift; my $s = sprintf('%.4f', $v); $s =~ s/0+$//; $s =~ s/\.$//; $s =~ s/^(-?)0\./$1./; $s = '0' if $s eq '' || $s eq '-'; return $s; }
sub fuente { my $txt = shift; $txt =~ s{(?<![\w.-])(-?)(\d*\.?\d+)rem\b}{ $nf++; my $px = 12 * $2 + 1; $1 . fmt($px / 16) . "\x02" }ge; return $txt; }
sub resto  { my $txt = shift; $txt =~ s{(?<![\w.-])(-?)(\d*\.?\d+)rem\b}{ $nr++; $1 . fmt(0.75 * $2) . 'rem' }ge; return $txt; }
sub convertir { my $txt = shift;
    $txt =~ s#(font-size\s*:\s*)([^;}"]+)#$1 . fuente($2)#ge;
    $txt = resto($txt);
    $txt =~ s/\x02/rem/g; return $txt; }

if ($modo eq 'xhtml') {
    # Solo dentro de atributos style="..."
    $c =~ s{(\bstyle=")([^"]*)(")}{ $1 . convertir($2) . $3 }ge;
} else {
    $c = convertir($c);
}
print STDERR "font-size=$nf otros=$nr\n";
print $c;
