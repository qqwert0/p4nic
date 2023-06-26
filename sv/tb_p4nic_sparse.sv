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
    wire      [31:0]    io_statusReg_0                ;
    wire      [31:0]    io_statusReg_1                ;
    wire      [31:0]    io_statusReg_2                ;
    wire      [31:0]    io_statusReg_3                ;
    wire      [31:0]    io_statusReg_4                ;
    wire      [31:0]    io_statusReg_5                ;
    wire      [31:0]    io_statusReg_6                ;
    wire      [31:0]    io_statusReg_7                ;
    wire      [31:0]    io_statusReg_8                ;
    wire      [31:0]    io_statusReg_9                ;
    wire      [31:0]    io_statusReg_10               ;
    wire      [31:0]    io_statusReg_11               ;
    wire      [31:0]    io_statusReg_12               ;
    wire      [31:0]    io_statusReg_13               ;
    wire      [31:0]    io_statusReg_14               ;
    wire      [31:0]    io_statusReg_15               ;
    wire      [31:0]    io_statusReg_16               ;
    wire      [31:0]    io_statusReg_17               ;
    wire      [31:0]    io_statusReg_18               ;
    wire      [31:0]    io_statusReg_19               ;
    wire      [31:0]    io_statusReg_20               ;
    wire      [31:0]    io_statusReg_21               ;
    wire      [31:0]    io_statusReg_22               ;
    wire      [31:0]    io_statusReg_23               ;
    wire      [31:0]    io_statusReg_24               ;
    wire      [31:0]    io_statusReg_25               ;
    wire      [31:0]    io_statusReg_26               ;
    wire      [31:0]    io_statusReg_27               ;
    wire      [31:0]    io_statusReg_28               ;
    wire      [31:0]    io_statusReg_29               ;
    wire      [31:0]    io_statusReg_30               ;
    wire      [31:0]    io_statusReg_31               ;
    wire      [31:0]    io_statusReg_32               ;
    wire      [31:0]    io_statusReg_33               ;
    wire      [31:0]    io_statusReg_34               ;
    wire      [31:0]    io_statusReg_35               ;
    wire      [31:0]    io_statusReg_36               ;
    wire      [31:0]    io_statusReg_37               ;
    wire      [31:0]    io_statusReg_38               ;
    wire      [31:0]    io_statusReg_39               ;
    wire      [31:0]    io_statusReg_40               ;
    wire      [31:0]    io_statusReg_41               ;
    wire      [31:0]    io_statusReg_42               ;
    wire      [31:0]    io_statusReg_43               ;
    wire      [31:0]    io_statusReg_44               ;
    wire      [31:0]    io_statusReg_45               ;
    wire      [31:0]    io_statusReg_46               ;
    wire      [31:0]    io_statusReg_47               ;
    wire      [31:0]    io_statusReg_48               ;
    wire      [31:0]    io_statusReg_49               ;
    wire      [31:0]    io_statusReg_50               ;
    wire      [31:0]    io_statusReg_51               ;
    wire      [31:0]    io_statusReg_52               ;
    wire      [31:0]    io_statusReg_53               ;
    wire      [31:0]    io_statusReg_54               ;
    wire      [31:0]    io_statusReg_55               ;
    wire      [31:0]    io_statusReg_56               ;
    wire      [31:0]    io_statusReg_57               ;
    wire      [31:0]    io_statusReg_58               ;
    wire      [31:0]    io_statusReg_59               ;
    wire      [31:0]    io_statusReg_60               ;
    wire      [31:0]    io_statusReg_61               ;
    wire      [31:0]    io_statusReg_62               ;
    wire      [31:0]    io_statusReg_63               ;
    wire      [31:0]    io_statusReg_64               ;
    wire      [31:0]    io_statusReg_65               ;
    wire      [31:0]    io_statusReg_66               ;
    wire      [31:0]    io_statusReg_67               ;
    wire      [31:0]    io_statusReg_68               ;
    wire      [31:0]    io_statusReg_69               ;
    wire      [31:0]    io_statusReg_70               ;
    wire      [31:0]    io_statusReg_71               ;
    wire      [31:0]    io_statusReg_72               ;
    wire      [31:0]    io_statusReg_73               ;
    wire      [31:0]    io_statusReg_74               ;
    wire      [31:0]    io_statusReg_75               ;
    wire      [31:0]    io_statusReg_76               ;
    wire      [31:0]    io_statusReg_77               ;
    wire      [31:0]    io_statusReg_78               ;
    wire      [31:0]    io_statusReg_79               ;
    wire      [31:0]    io_statusReg_80               ;
    wire      [31:0]    io_statusReg_81               ;
    wire      [31:0]    io_statusReg_82               ;
    wire      [31:0]    io_statusReg_83               ;
    wire      [31:0]    io_statusReg_84               ;
    wire      [31:0]    io_statusReg_85               ;
    wire      [31:0]    io_statusReg_86               ;
    wire      [31:0]    io_statusReg_87               ;
    wire      [31:0]    io_statusReg_88               ;
    wire      [31:0]    io_statusReg_89               ;
    wire      [31:0]    io_statusReg_90               ;
    wire      [31:0]    io_statusReg_91               ;
    wire      [31:0]    io_statusReg_92               ;
    wire      [31:0]    io_statusReg_93               ;
    wire      [31:0]    io_statusReg_94               ;
    wire      [31:0]    io_statusReg_95               ;
    wire      [31:0]    io_statusReg_96               ;
    wire      [31:0]    io_statusReg_97               ;
    wire      [31:0]    io_statusReg_98               ;
    wire      [31:0]    io_statusReg_99               ;
    wire      [31:0]    io_statusReg_100              ;
    wire      [31:0]    io_statusReg_101              ;
    wire      [31:0]    io_statusReg_102              ;
    wire      [31:0]    io_statusReg_103              ;
    wire      [31:0]    io_statusReg_104              ;
    wire      [31:0]    io_statusReg_105              ;
    wire      [31:0]    io_statusReg_106              ;
    wire      [31:0]    io_statusReg_107              ;
    wire      [31:0]    io_statusReg_108              ;
    wire      [31:0]    io_statusReg_109              ;
    wire      [31:0]    io_statusReg_110              ;
    wire      [31:0]    io_statusReg_111              ;
    wire      [31:0]    io_statusReg_112              ;
    wire      [31:0]    io_statusReg_113              ;
    wire      [31:0]    io_statusReg_114              ;
    wire      [31:0]    io_statusReg_115              ;
    wire      [31:0]    io_statusReg_116              ;
    wire      [31:0]    io_statusReg_117              ;
    wire      [31:0]    io_statusReg_118              ;
    wire      [31:0]    io_statusReg_119              ;
    wire      [31:0]    io_statusReg_120              ;
    wire      [31:0]    io_statusReg_121              ;
    wire      [31:0]    io_statusReg_122              ;
    wire      [31:0]    io_statusReg_123              ;
    wire      [31:0]    io_statusReg_124              ;
    wire      [31:0]    io_statusReg_125              ;
    wire      [31:0]    io_statusReg_126              ;
    wire      [31:0]    io_statusReg_127              ;



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
     * Reg(56)    : dataTotalLen
     * Reg(57)    : IdxTransNum.
     * Reg(58)    : rxIdxInitAddr.
     * Reg(59)    : rxDataInitAddr.
     * Reg(60)    : idxTotalLen
     * Reg(61)    : nodeRank.
     * Reg(62)    : engineRand.
     * Reg(63)    : RxIdxDepth.

     */

    int index_row0 = 10;
    int block_num0 = 1027;     
    int index_row1 = 10;
    int block_num1 = 1027;
    int block_byte = 128;
    int ONCE_TX_BLOCK = 1024;
    int ONCE_RX_ROW = 64;
    int ENGINE_NUM = 256;


initial begin
    reset <= 1;
    clock = 1;
    #1000;
    reset <= 0;
    #10
    // qdma0.init_incr(32'd0,32'd9472,32'd5); //int start_addr, int length, int offset
    qdma0.init_from_file("/home/amax/hhj/chisel_4p4nic/p4nic/sv/a2.txt",160);//path, line numbers
    qdma1.init_from_file("/home/amax/hhj/chisel_4p4nic/p4nic/sv/a2.txt",160);//path, line numbers
    // qdma0.init_incr(32'd27648,32'd16384,32'd5); //int start_addr, int length, int offset
    #100;
    io_controlReg_0_20   <= 8;
    io_controlReg_0_21   <= 16;
    io_controlReg_1_20   <= 8;
    io_controlReg_1_21   <= 16;
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
    io_controlReg_0_38   <= ONCE_TX_BLOCK * 4 + ONCE_TX_BLOCK * block_byte;//2packet memread len 135168=2*67584 66KB=2Kb+ 64Kb
    io_controlReg_0_40   <= 0;
    io_controlReg_0_41   <= 0;
    io_controlReg_0_42   <= 0;
    io_controlReg_0_56   <= block_byte * block_num0/64;//block_byte * block_num/64;
    io_controlReg_0_57   <= ONCE_TX_BLOCK;
    io_controlReg_0_58   <= 32'h1000_4444;
    io_controlReg_0_59   <= 32'h1000_5555;
    io_controlReg_0_60   <= index_row0 * ENGINE_NUM/16;
    io_controlReg_0_63   <= ONCE_RX_ROW;
    io_controlReg_1_28   <= 0;
    io_controlReg_1_29   <= 0;
    io_controlReg_1_30   <= 0;
    io_controlReg_1_32   <= 0;
    io_controlReg_1_33   <= 0;
    io_controlReg_1_34   <= 0;
    io_controlReg_1_36   <= 0;
    io_controlReg_1_37   <= 0;
    io_controlReg_1_38   <= ONCE_TX_BLOCK * 4 + ONCE_TX_BLOCK * block_byte;
    io_controlReg_1_40   <= 0;
    io_controlReg_1_41   <= 0;
    io_controlReg_1_42   <= 0;
    io_controlReg_1_56   <= block_byte * block_num1/64;
    io_controlReg_1_57   <= ONCE_TX_BLOCK;
    io_controlReg_1_58   <= 32'h1000_4444;
    io_controlReg_1_59   <= 32'h1000_5555;    
    io_controlReg_1_60   <= index_row1 * ENGINE_NUM/16;  
    io_controlReg_1_61   <= 0;
    io_controlReg_1_62   <= 0;
    io_controlReg_1_63   <= ONCE_RX_ROW;     
    #50;
    io_controlReg_0_42   <= 1;  //read qdma0 memory
    io_controlReg_1_42   <= 1;  //read qdma1 memory
    #50
    io_controlReg_0_42   <= 0;  //write qdma0 memory
    io_controlReg_1_42   <= 0;  //write qdma1 memory
    #150000
    io_controlReg_0_36   <= ONCE_TX_BLOCK *4;//memread phys address
    io_controlReg_0_38   <= 4096 + 384;
    io_controlReg_0_42   <= 1;  //read qdma0 memory
    io_controlReg_1_36   <= ONCE_TX_BLOCK *4;//memread phys address
    io_controlReg_1_38   <= 4096 + 384;
    io_controlReg_1_42   <= 1;  //read qdma0 memory

    #20
    io_controlReg_0_42   <= 0;  //read qdma0 memory
    io_controlReg_1_42   <= 0;  //read qdma0 memory
    #150000
    io_controlReg_0_36   <= ONCE_TX_BLOCK *8;//memread phys address
    io_controlReg_0_38   <= 2048;
    io_controlReg_0_42   <= 1;  //read qdma0 memory
    io_controlReg_1_36   <= ONCE_TX_BLOCK *8;//memread phys address
    io_controlReg_1_38   <= 2048;
    io_controlReg_1_42   <= 1;  //read qdma0 memory
    // #20
    // io_controlReg_0_42   <= 0;  //read qdma0 memory
    // io_controlReg_1_42   <= 0;  //read qdma0 memory
    // #150000
    // io_controlReg_0_36   <= ONCE_TX_BLOCK *12;//memread phys address
    // io_controlReg_0_42   <= 1;  //read qdma0 memory
    // io_controlReg_1_36   <= ONCE_TX_BLOCK *12;//memread phys address
    // io_controlReg_1_42   <= 1;  //read qdma0 memory
    // #20
    // io_controlReg_0_42   <= 0;  //read qdma0 memory
    // io_controlReg_1_42   <= 0;  //read qdma0 memory
    // #150000
    // io_controlReg_0_36   <= ONCE_TX_BLOCK *16;//memread phys address
    // io_controlReg_0_42   <= 1;  //read qdma0 memory
    // io_controlReg_1_36   <= ONCE_TX_BLOCK *16;//memread phys address
    // io_controlReg_1_42   <= 1;  //read qdma0 memory
    // #20
    // io_controlReg_0_42   <= 0;  //read qdma0 memory
    // io_controlReg_1_42   <= 0;  //read qdma0 memory
    // #150000
    // io_controlReg_0_36   <= ONCE_TX_BLOCK *20;//memread phys address
    // io_controlReg_0_42   <= 1;  //read qdma0 memory
    // io_controlReg_1_36   <= ONCE_TX_BLOCK *20;//memread phys address
    // io_controlReg_1_42   <= 1;  //read qdma0 memory
    // #20
    // io_controlReg_0_42   <= 0;  //read qdma0 memory
    // io_controlReg_1_42   <= 0;  //read qdma0 memory
    // #150000
    // io_controlReg_0_36   <= ONCE_TX_BLOCK *24;//memread phys address
    // // io_controlReg_0_38   <= 3072 + 89856;//2packet memread len 
    // io_controlReg_0_42   <= 1;  //read qdma0 memory
    // io_controlReg_1_36   <= ONCE_TX_BLOCK *24;//memread phys address
    // // io_controlReg_1_38   <= 3072 + 89856;//2packet memread len 
    // io_controlReg_1_42   <= 1;  //read qdma0 memory
    // #20
    // io_controlReg_0_42   <= 0;  //read qdma0 memory
    // io_controlReg_1_42   <= 0;  //read qdma0 memory
    // #150000
    // io_controlReg_0_36   <= ONCE_TX_BLOCK *28;//memread phys address
    // io_controlReg_0_42   <= 1;  //read qdma0 memory
    // io_controlReg_1_36   <= ONCE_TX_BLOCK *28;//memread phys address
    // io_controlReg_1_42   <= 1;  //read qdma0 memory
    // #20
    // io_controlReg_0_42   <= 0;  //read qdma0 memory
    // io_controlReg_1_42   <= 0;  //read qdma0 memory
    // #150000
    // io_controlReg_0_36   <= ONCE_TX_BLOCK *32;//memread phys address
    // io_controlReg_0_42   <= 1;  //read qdma0 memory
    // io_controlReg_1_36   <= ONCE_TX_BLOCK *32;//memread phys address
    // io_controlReg_1_42   <= 1;  //read qdma0 memory


end
always #5 clock=~clock;

endmodule