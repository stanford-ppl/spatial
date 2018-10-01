design=pr_region_alternate

# regenerate qsys ip files
qsys-generate ${design}.qsys --block-symbol-file --family="Arria 10" --part=10AS066N3F40E2SG --quartus-project=ghrd_10as066n2.qpf --rev=pr_alternate_synth
qsys-generate ${design}.qsys --synthesis=VERILOG --family="Arria 10" --part=10AS066N3F40E2SG --quartus-project=ghrd_10as066n2.qpf --rev=pr_alternate_synth

# regenerate ips and resynthesize the design
quartus_ipgenerate --run_default_mode_op ghrd_10as066n2 -c pr_alternate_synth
quartus_syn --read_settings_files=on --write_settings_files=off ghrd_10as066n2 -c pr_alternate_synth

# implement the design in pr_alternate_fit
quartus_cdb ghrd_10as066n2 -c pr_alternate_synth --export_block root_partition --snapshot synthesized --file pr_alternate_synth.qdb
quartus_cdb ghrd_10as066n2 -c pr_alternate_fit --import_block root_partition --file pr_base_static.qdb
quartus_cdb ghrd_10as066n2 -c pr_alternate_fit --import_block pr_region --file pr_alternate_synth.qdb
quartus_fit ghrd_10as066n2 -c pr_alternate_fit
quartus_sta ghrd_10as066n2 -c pr_alternate_fit
quartus_asm ghrd_10as066n2 -c pr_alternate_fit

# generate rbf for the alternate image
quartus_cpf -c output_files/pr_alternate_fit.pr_region.pmsf output_files/pr_region_alt.rbf
