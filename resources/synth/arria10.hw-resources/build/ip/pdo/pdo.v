module pdo
(
	i, oein, o, obar, oebout, oeout 
);

input i;
input oein;
output o;
output obar;
output wire oebout;
output wire oeout;

twentynm_pseudo_diff_out pdo_wys
(
	.i(i),
	.oein(oein),
	.o(o),
	.oebout(oebout),
	.oeout(oeout),
	.dtc(),
	.dtcbar(),
	.dtcin(),
	.obar(obar)
);

endmodule