set_property PACKAGE_PIN D32 [get_ports hbmCattrip]
set_property IOSTANDARD  LVCMOS18 [get_ports hbmCattrip]

create_clock -name sys_100M_clock_0 -period 10 -add [get_ports sysClkP]

set_property PACKAGE_PIN BJ43 [get_ports sysClkP]
set_property PACKAGE_PIN BJ44 [get_ports sysClkN]
set_property IOSTANDARD DIFF_SSTL12 [get_ports sysClkP]
set_property IOSTANDARD DIFF_SSTL12 [get_ports sysClkN]

set_false_path -from [get_clocks -of_objects [get_pins -hier -filter {NAME =~ */gtye4_channel_gen.gen_gtye4_channel_inst[*].GTYE4_CHANNEL_PRIM_INST/RXOUTCLK}]] -to [get_clocks -of_objects [get_pins -hier -filter {NAME =~ */gtye4_channel_gen.gen_gtye4_channel_inst[*].GTYE4_CHANNEL_PRIM_INST/TXOUTCLK}]]

create_clock -name sys_clk -period 10 [get_ports qdmaPin_sys_clk_p]

set_property USER_SLR_ASSIGNMENT SLR0 [get_cells {qdmaInst}]

set_false_path -from [get_ports qdmaPin_sys_rst_n]
set_property PULLUP true [get_ports qdmaPin_sys_rst_n]
set_property IOSTANDARD LVCMOS18 [get_ports qdmaPin_sys_rst_n]
set_property PACKAGE_PIN BH26 [get_ports qdmaPin_sys_rst_n]
set_property CONFIG_VOLTAGE 1.8 [current_design]

set_property LOC [get_package_pins -of_objects [get_bels [get_sites -filter {NAME =~ *COMMON*} -of_objects [get_iobanks -of_objects [get_sites GTYE4_CHANNEL_X1Y7]]]/REFCLK0P]] [get_ports qdmaPin_sys_clk_p]
set_property LOC [get_package_pins -of_objects [get_bels [get_sites -filter {NAME =~ *COMMON*} -of_objects [get_iobanks -of_objects [get_sites GTYE4_CHANNEL_X1Y7]]]/REFCLK0N]] [get_ports qdmaPin_sys_clk_n]

set_false_path -from [get_cells -regexp {qdmaInst/axil2reg/reg_control_[0-9]*_reg\[.*]}]
set_false_path -to [get_cells -regexp {qdmaInst/axil2reg/reg_status_[0-9]*_reg\[.*]}]
#reg_control_0_reg[0]
#set_false_path -from [get_cells qdma/axil2reg/reg_control_[*]]
#set_false_path -to [get_cells qdma/axil_reg_0/reg_status_[*]]

###
set_false_path -to [get_pins -hier *sync_reg[0]/D]
###
set_property C_USER_SCAN_CHAIN 1 [get_debug_cores dbg_hub]

# Enable if you import QDMA and CMAC at the same time

set_false_path -from [get_clocks -of_objects [get_pins -hier -filter {NAME =~ */phy_clk_i/bufg_gt_userclk/O}]] -to [get_clocks -of_objects [get_pins -hier -filter {NAME =~ */gtye4_channel_gen.gen_gtye4_channel_inst[*].GTYE4_CHANNEL_PRIM_INST/TXOUTCLK}]]
set_false_path -from [get_clocks -of_objects [get_pins -hier -filter {NAME =~ */gtye4_channel_gen.gen_gtye4_channel_inst[*].GTYE4_CHANNEL_PRIM_INST/TXOUTCLK}]] -to [get_clocks -of_objects [get_pins -hier -filter {NAME =~ */phy_clk_i/bufg_gt_userclk/O}]]

create_pblock pblock_cmac
add_cells_to_pblock [get_pblocks pblock_cmac] [get_cells -quiet [list cmacInst]]
resize_pblock [get_pblocks pblock_cmac] -add {SLR2}