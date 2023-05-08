module testbench_P4nicSpase(

    );

    reg                 clock                         =0;
    reg                 reset                         =0;
    reg                 io_c2h_cmd_0_ready            =0;
    wire                io_c2h_cmd_0_valid            ;
    wire      [63:0]    io_c2h_cmd_0_bits_addr        ;
    wire      [10:0]    io_c2h_cmd_0_bits_qid         ;
    wire                io_c2h_cmd_0_bits_error       ;
    wire      [7:0]     io_c2h_cmd_0_bits_func        ;
    wire      [2:0]     io_c2h_cmd_0_bits_port_id     ;
    wire      [6:0]     io_c2h_cmd_0_bits_pfch_tag    ;
    wire      [31:0]    io_c2h_cmd_0_bits_len         ;
    reg                 io_c2h_cmd_1_ready            =0;
    wire                io_c2h_cmd_1_valid            ;
    wire      [63:0]    io_c2h_cmd_1_bits_addr        ;
    wire      [10:0]    io_c2h_cmd_1_bits_qid         ;
    wire                io_c2h_cmd_1_bits_error       ;
    wire      [7:0]     io_c2h_cmd_1_bits_func        ;
    wire      [2:0]     io_c2h_cmd_1_bits_port_id     ;
    wire      [6:0]     io_c2h_cmd_1_bits_pfch_tag    ;
    wire      [31:0]    io_c2h_cmd_1_bits_len         ;
    reg                 io_c2h_data_0_ready           =0;
    wire                io_c2h_data_0_valid           ;
    wire      [511:0]   io_c2h_data_0_bits_data       ;
    wire      [31:0]    io_c2h_data_0_bits_tcrc       ;
    wire                io_c2h_data_0_bits_ctrl_marker;
    wire      [6:0]     io_c2h_data_0_bits_ctrl_ecc   ;
    wire      [31:0]    io_c2h_data_0_bits_ctrl_len   ;
    wire      [2:0]     io_c2h_data_0_bits_ctrl_port_id;
    wire      [10:0]    io_c2h_data_0_bits_ctrl_qid   ;
    wire                io_c2h_data_0_bits_ctrl_has_cmpt;
    wire                io_c2h_data_0_bits_last       ;
    wire      [5:0]     io_c2h_data_0_bits_mty        ;
    reg                 io_c2h_data_1_ready           =0;
    wire                io_c2h_data_1_valid           ;
    wire      [511:0]   io_c2h_data_1_bits_data       ;
    wire      [31:0]    io_c2h_data_1_bits_tcrc       ;
    wire                io_c2h_data_1_bits_ctrl_marker;
    wire      [6:0]     io_c2h_data_1_bits_ctrl_ecc   ;
    wire      [31:0]    io_c2h_data_1_bits_ctrl_len   ;
    wire      [2:0]     io_c2h_data_1_bits_ctrl_port_id;
    wire      [10:0]    io_c2h_data_1_bits_ctrl_qid   ;
    wire                io_c2h_data_1_bits_ctrl_has_cmpt;
    wire                io_c2h_data_1_bits_last       ;
    wire      [5:0]     io_c2h_data_1_bits_mty        ;
    reg                 io_h2c_cmd_0_ready            =0;
    wire                io_h2c_cmd_0_valid            ;
    wire      [63:0]    io_h2c_cmd_0_bits_addr        ;
    wire      [31:0]    io_h2c_cmd_0_bits_len         ;
    wire                io_h2c_cmd_0_bits_eop         ;
    wire                io_h2c_cmd_0_bits_sop         ;
    wire                io_h2c_cmd_0_bits_mrkr_req    ;
    wire                io_h2c_cmd_0_bits_sdi         ;
    wire      [10:0]    io_h2c_cmd_0_bits_qid         ;
    wire                io_h2c_cmd_0_bits_error       ;
    wire      [7:0]     io_h2c_cmd_0_bits_func        ;
    wire      [15:0]    io_h2c_cmd_0_bits_cidx        ;
    wire      [2:0]     io_h2c_cmd_0_bits_port_id     ;
    wire                io_h2c_cmd_0_bits_no_dma      ;
    reg                 io_h2c_cmd_1_ready            =0;
    wire                io_h2c_cmd_1_valid            ;
    wire      [63:0]    io_h2c_cmd_1_bits_addr        ;
    wire      [31:0]    io_h2c_cmd_1_bits_len         ;
    wire                io_h2c_cmd_1_bits_eop         ;
    wire                io_h2c_cmd_1_bits_sop         ;
    wire                io_h2c_cmd_1_bits_mrkr_req    ;
    wire                io_h2c_cmd_1_bits_sdi         ;
    wire      [10:0]    io_h2c_cmd_1_bits_qid         ;
    wire                io_h2c_cmd_1_bits_error       ;
    wire      [7:0]     io_h2c_cmd_1_bits_func        ;
    wire      [15:0]    io_h2c_cmd_1_bits_cidx        ;
    wire      [2:0]     io_h2c_cmd_1_bits_port_id     ;
    wire                io_h2c_cmd_1_bits_no_dma      ;
    wire                io_h2c_data_0_ready           ;
    reg                 io_h2c_data_0_valid           =0;
    reg       [511:0]   io_h2c_data_0_bits_data       =0;
    reg       [31:0]    io_h2c_data_0_bits_tcrc       =0;
    reg       [10:0]    io_h2c_data_0_bits_tuser_qid  =0;
    reg       [2:0]     io_h2c_data_0_bits_tuser_port_id=0;
    reg                 io_h2c_data_0_bits_tuser_err  =0;
    reg       [31:0]    io_h2c_data_0_bits_tuser_mdata=0;
    reg       [5:0]     io_h2c_data_0_bits_tuser_mty  =0;
    reg                 io_h2c_data_0_bits_tuser_zero_byte=0;
    reg                 io_h2c_data_0_bits_last       =0;
    wire                io_h2c_data_1_ready           ;
    reg                 io_h2c_data_1_valid           =0;
    reg       [511:0]   io_h2c_data_1_bits_data       =0;
    reg       [31:0]    io_h2c_data_1_bits_tcrc       =0;
    reg       [10:0]    io_h2c_data_1_bits_tuser_qid  =0;
    reg       [2:0]     io_h2c_data_1_bits_tuser_port_id=0;
    reg                 io_h2c_data_1_bits_tuser_err  =0;
    reg       [31:0]    io_h2c_data_1_bits_tuser_mdata=0;
    reg       [5:0]     io_h2c_data_1_bits_tuser_mty  =0;
    reg                 io_h2c_data_1_bits_tuser_zero_byte=0;
    reg                 io_h2c_data_1_bits_last       =0;
    reg       [31:0]    io_controlReg_0_0             =0;
    reg       [31:0]    io_controlReg_0_1             =0;
    reg       [31:0]    io_controlReg_0_2             =0;
    reg       [31:0]    io_controlReg_0_3             =0;
    reg       [31:0]    io_controlReg_0_4             =0;
    reg       [31:0]    io_controlReg_0_5             =0;
    reg       [31:0]    io_controlReg_0_6             =0;
    reg       [31:0]    io_controlReg_0_7             =0;
    reg       [31:0]    io_controlReg_0_8             =0;
    reg       [31:0]    io_controlReg_0_9             =0;
    reg       [31:0]    io_controlReg_0_10            =0;
    reg       [31:0]    io_controlReg_0_11            =0;
    reg       [31:0]    io_controlReg_0_12            =0;
    reg       [31:0]    io_controlReg_0_13            =0;
    reg       [31:0]    io_controlReg_0_14            =0;
    reg       [31:0]    io_controlReg_0_15            =0;
    reg       [31:0]    io_controlReg_0_16            =0;
    reg       [31:0]    io_controlReg_0_17            =0;
    reg       [31:0]    io_controlReg_0_18            =0;
    reg       [31:0]    io_controlReg_0_19            =0;
    reg       [31:0]    io_controlReg_0_20            =0;
    reg       [31:0]    io_controlReg_0_21            =0;
    reg       [31:0]    io_controlReg_0_22            =0;
    reg       [31:0]    io_controlReg_0_23            =0;
    reg       [31:0]    io_controlReg_0_24            =0;
    reg       [31:0]    io_controlReg_0_25            =0;
    reg       [31:0]    io_controlReg_0_26            =0;
    reg       [31:0]    io_controlReg_0_27            =0;
    reg       [31:0]    io_controlReg_0_28            =0;
    reg       [31:0]    io_controlReg_0_29            =0;
    reg       [31:0]    io_controlReg_0_30            =0;
    reg       [31:0]    io_controlReg_0_31            =0;
    reg       [31:0]    io_controlReg_0_32            =0;
    reg       [31:0]    io_controlReg_0_33            =0;
    reg       [31:0]    io_controlReg_0_34            =0;
    reg       [31:0]    io_controlReg_0_35            =0;
    reg       [31:0]    io_controlReg_0_36            =0;
    reg       [31:0]    io_controlReg_0_37            =0;
    reg       [31:0]    io_controlReg_0_38            =0;
    reg       [31:0]    io_controlReg_0_39            =0;
    reg       [31:0]    io_controlReg_0_40            =0;
    reg       [31:0]    io_controlReg_0_41            =0;
    reg       [31:0]    io_controlReg_0_42            =0;
    reg       [31:0]    io_controlReg_0_43            =0;
    reg       [31:0]    io_controlReg_0_44            =0;
    reg       [31:0]    io_controlReg_0_45            =0;
    reg       [31:0]    io_controlReg_0_46            =0;
    reg       [31:0]    io_controlReg_0_47            =0;
    reg       [31:0]    io_controlReg_0_48            =0;
    reg       [31:0]    io_controlReg_0_49            =0;
    reg       [31:0]    io_controlReg_0_50            =0;
    reg       [31:0]    io_controlReg_0_51            =0;
    reg       [31:0]    io_controlReg_0_52            =0;
    reg       [31:0]    io_controlReg_0_53            =0;
    reg       [31:0]    io_controlReg_0_54            =0;
    reg       [31:0]    io_controlReg_0_55            =0;
    reg       [31:0]    io_controlReg_0_56            =0;
    reg       [31:0]    io_controlReg_0_57            =0;
    reg       [31:0]    io_controlReg_0_58            =0;
    reg       [31:0]    io_controlReg_0_59            =0;
    reg       [31:0]    io_controlReg_0_60            =0;
    reg       [31:0]    io_controlReg_0_61            =0;
    reg       [31:0]    io_controlReg_0_62            =0;
    reg       [31:0]    io_controlReg_0_63            =0;
    reg       [31:0]    io_controlReg_1_0             =0;
    reg       [31:0]    io_controlReg_1_1             =0;
    reg       [31:0]    io_controlReg_1_2             =0;
    reg       [31:0]    io_controlReg_1_3             =0;
    reg       [31:0]    io_controlReg_1_4             =0;
    reg       [31:0]    io_controlReg_1_5             =0;
    reg       [31:0]    io_controlReg_1_6             =0;
    reg       [31:0]    io_controlReg_1_7             =0;
    reg       [31:0]    io_controlReg_1_8             =0;
    reg       [31:0]    io_controlReg_1_9             =0;
    reg       [31:0]    io_controlReg_1_10            =0;
    reg       [31:0]    io_controlReg_1_11            =0;
    reg       [31:0]    io_controlReg_1_12            =0;
    reg       [31:0]    io_controlReg_1_13            =0;
    reg       [31:0]    io_controlReg_1_14            =0;
    reg       [31:0]    io_controlReg_1_15            =0;
    reg       [31:0]    io_controlReg_1_16            =0;
    reg       [31:0]    io_controlReg_1_17            =0;
    reg       [31:0]    io_controlReg_1_18            =0;
    reg       [31:0]    io_controlReg_1_19            =0;
    reg       [31:0]    io_controlReg_1_20            =0;
    reg       [31:0]    io_controlReg_1_21            =0;
    reg       [31:0]    io_controlReg_1_22            =0;
    reg       [31:0]    io_controlReg_1_23            =0;
    reg       [31:0]    io_controlReg_1_24            =0;
    reg       [31:0]    io_controlReg_1_25            =0;
    reg       [31:0]    io_controlReg_1_26            =0;
    reg       [31:0]    io_controlReg_1_27            =0;
    reg       [31:0]    io_controlReg_1_28            =0;
    reg       [31:0]    io_controlReg_1_29            =0;
    reg       [31:0]    io_controlReg_1_30            =0;
    reg       [31:0]    io_controlReg_1_31            =0;
    reg       [31:0]    io_controlReg_1_32            =0;
    reg       [31:0]    io_controlReg_1_33            =0;
    reg       [31:0]    io_controlReg_1_34            =0;
    reg       [31:0]    io_controlReg_1_35            =0;
    reg       [31:0]    io_controlReg_1_36            =0;
    reg       [31:0]    io_controlReg_1_37            =0;
    reg       [31:0]    io_controlReg_1_38            =0;
    reg       [31:0]    io_controlReg_1_39            =0;
    reg       [31:0]    io_controlReg_1_40            =0;
    reg       [31:0]    io_controlReg_1_41            =0;
    reg       [31:0]    io_controlReg_1_42            =0;
    reg       [31:0]    io_controlReg_1_43            =0;
    reg       [31:0]    io_controlReg_1_44            =0;
    reg       [31:0]    io_controlReg_1_45            =0;
    reg       [31:0]    io_controlReg_1_46            =0;
    reg       [31:0]    io_controlReg_1_47            =0;
    reg       [31:0]    io_controlReg_1_48            =0;
    reg       [31:0]    io_controlReg_1_49            =0;
    reg       [31:0]    io_controlReg_1_50            =0;
    reg       [31:0]    io_controlReg_1_51            =0;
    reg       [31:0]    io_controlReg_1_52            =0;
    reg       [31:0]    io_controlReg_1_53            =0;
    reg       [31:0]    io_controlReg_1_54            =0;
    reg       [31:0]    io_controlReg_1_55            =0;
    reg       [31:0]    io_controlReg_1_56            =0;
    reg       [31:0]    io_controlReg_1_57            =0;
    reg       [31:0]    io_controlReg_1_58            =0;
    reg       [31:0]    io_controlReg_1_59            =0;
    reg       [31:0]    io_controlReg_1_60            =0;
    reg       [31:0]    io_controlReg_1_61            =0;
    reg       [31:0]    io_controlReg_1_62            =0;
    reg       [31:0]    io_controlReg_1_63            =0;
    wire      [63:0]    h2c_data_0_keep;
    wire      [63:0]    h2c_data_1_keep;

DMA #(512) qdma0(
    clock,
    reset,
    //DMA CMD streams
    io_h2c_cmd_0_valid,
    io_h2c_cmd_0_ready,
    io_h2c_cmd_0_bits_addr,
    io_h2c_cmd_0_bits_len,
    io_c2h_cmd_0_valid,
    io_c2h_cmd_0_ready,
    io_c2h_cmd_0_bits_addr,
    io_c2h_cmd_0_bits_len,        
    //DMA Data streams      
    io_h2c_data_0_valid,
    io_h2c_data_0_ready,
    io_h2c_data_0_bits_data,
    h2c_data_0_keep,
    io_h2c_data_0_bits_last,
    io_c2h_data_0_valid,
    io_c2h_data_0_ready,
    io_c2h_data_0_bits_data,
    64'hffff_ffff_ffff_ffff,
    io_c2h_data_0_bits_last        
);

DMA #(512) qdma1(
    clock,
    reset,
    //DMA CMD streams
    io_h2c_cmd_1_valid,
    io_h2c_cmd_1_ready,
    io_h2c_cmd_1_bits_addr,
    io_h2c_cmd_1_bits_len,
    io_c2h_cmd_1_valid,
    io_c2h_cmd_1_ready,
    io_c2h_cmd_1_bits_addr,
    io_c2h_cmd_1_bits_len,        
    //DMA Data streams      
    io_h2c_data_1_valid,
    io_h2c_data_1_ready,
    io_h2c_data_1_bits_data,
    h2c_data_1_keep,
    io_h2c_data_1_bits_last,
    io_c2h_data_1_valid,
    io_c2h_data_1_ready,
    io_c2h_data_1_bits_data,
    64'hffff_ffff_ffff_ffff,
    io_c2h_data_1_bits_last        
);

P4nicSpase P4nicSpase_inst(
        .*
);

    /* For worker, QDMA regs are used as below:
     *
     * Reg(28-29): memWrite phys address
     * Reg(30)    : memWrite len
     * Reg(32-33): memWrite req callback
     * Reg(34)    : memWrite req valid
     * Reg(36-37): memRead phys address
     * Reg(38)    : memRead len
     * Reg(40-41): memRead req callback
     * Reg(42)    : memRead req valid
     * Reg(60)    : Index total length
     * Reg(61)    : Node rank
     * Reg(62)    : Engine rank
     */


initial begin
    reset <= 1;
    clock = 1;
    #1000;
    reset <= 0;
    #10
    // qdma0.init_incr(32'd0,32'd9472,32'd5); //int start_addr, int length, int offset
    qdma0.init_from_file("/home/amax/hhj/chisel_4p4nic/p4nic/sv/data.txt",12);//path, line numbers
    qdma1.init_from_file("/home/amax/hhj/chisel_4p4nic/p4nic/sv/data1.txt",12);//path, line numbers
     qdma0.init_incr(32'd2048,32'd16384,32'd5); //int start_addr, int length, int offset
    #100;
    io_controlReg_0_28   <= 0;//memwrite phys address
    io_controlReg_0_29   <= 0;
    io_controlReg_0_30   <= 0;//memwrite len
    //io_controlReg_0_30   <= 8256;//memwrite len
    io_controlReg_0_32   <= 0;
    io_controlReg_0_33   <= 0;
    io_controlReg_0_34   <= 0;
    io_controlReg_0_36   <= 0;//memread phys address
    io_controlReg_0_37   <= 0;
    //io_controlReg_0_38   <= 9472;//memread len 9472=8192+ 1280(10 data packet)
    //io_controlReg_0_38   <= 270336;//1packet memread len 270336 264KB=8KB+ 256KB
   // io_controlReg_0_38   <= 540672;//2packet memread len 540672=2*270336 264KB=8KB+ 256KB
    io_controlReg_0_38   <= 8448;//2packet memread len 135168=2*67584 66KB=2Kb+ 64Kb
    io_controlReg_0_40   <= 0;
    io_controlReg_0_41   <= 0;
    io_controlReg_0_42   <= 0;
    io_controlReg_0_56   <= 320;
    io_controlReg_0_57   <= 64;
    io_controlReg_0_58   <= 32'h4444;
    io_controlReg_0_59   <= 32'h5555;
    io_controlReg_0_60   <= 12;
    io_controlReg_0_63   <= 4;
    io_controlReg_1_28   <= 0;
    io_controlReg_1_29   <= 0;
    io_controlReg_1_30   <= 0;
    io_controlReg_1_32   <= 0;
    io_controlReg_1_33   <= 0;
    io_controlReg_1_34   <= 0;
    io_controlReg_1_36   <= 0;
    io_controlReg_1_37   <= 0;
    io_controlReg_1_38   <= 8448;
    io_controlReg_1_40   <= 0;
    io_controlReg_1_41   <= 0;
    io_controlReg_1_42   <= 0;
    io_controlReg_1_56   <= 320;
    io_controlReg_1_57   <= 64;
    io_controlReg_1_58   <= 32'h4444;
    io_controlReg_1_59   <= 32'h5555;    
    io_controlReg_1_60   <= 12;   
    io_controlReg_1_61   <= 0;
    io_controlReg_1_62   <= 0;
    io_controlReg_1_63   <= 4;     
    #50;
    io_controlReg_0_42   <= 1;  //read qdma0 memory
    io_controlReg_1_42   <= 1;  //read qdma1 memory
    #50
    io_controlReg_0_42   <= 0;  //write qdma0 memory
    io_controlReg_1_42   <= 0;  //write qdma1 memory
    #4200
    io_controlReg_0_36   <= 256;//memread phys address
    io_controlReg_0_37   <= 0;
    io_controlReg_0_38   <= 8448;//2packet memread len 135168=2*67584 66KB=2Kb+ 64Kb    
    io_controlReg_0_42   <= 1;  //read qdma0 memory
    io_controlReg_1_36   <= 256;//memread phys address
    io_controlReg_1_37   <= 0;
    io_controlReg_1_38   <= 8448;//2packet memread len 135168=2*67584 66KB=2Kb+ 64Kb    
    io_controlReg_1_42   <= 1;  //read qdma0 memory

    #20
    io_controlReg_0_42   <= 0;  //read qdma0 memory
    io_controlReg_1_42   <= 0;  //read qdma0 memory
    #4200
    io_controlReg_0_36   <= 512;//memread phys address
    io_controlReg_0_37   <= 0;
    io_controlReg_0_38   <= 4352;//2packet memread len 135168=2*67584 66KB=2Kb+ 64Kb    
    io_controlReg_0_42   <= 1;  //read qdma0 memory
    io_controlReg_1_36   <= 512;//memread phys address
    io_controlReg_1_37   <= 0;
    io_controlReg_1_38   <= 4352;//2packet memread len 135168=2*67584 66KB=2Kb+ 64Kb    
    io_controlReg_1_42   <= 1;  //read qdma0 memory


end
always #5 clock=~clock;

endmodule