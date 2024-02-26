module testbench_P4Arbiter(

    );

reg                 clock                         =0;
reg                 reset                         =0;
wire                io_in_0_ready                 ;
reg                 io_in_0_valid                 =0;
reg       [15:0]    io_in_0_bits_head_eth_type    =0;
reg       [31:0]    io_in_0_bits_head_next_idx    =0;
reg       [31:0]    io_in_0_bits_head_bitmap      =0;
reg       [31:0]    io_in_0_bits_head_index       =0;
reg                 io_in_0_bits_is_empty         =0;
wire                io_in_1_ready                 ;
reg                 io_in_1_valid                 =0;
reg       [15:0]    io_in_1_bits_head_eth_type    =0;
reg       [31:0]    io_in_1_bits_head_next_idx    =0;
reg       [31:0]    io_in_1_bits_head_bitmap      =0;
reg       [31:0]    io_in_1_bits_head_index       =0;
reg                 io_in_1_bits_is_empty         =0;
wire                io_in_2_ready                 ;
reg                 io_in_2_valid                 =0;
reg       [15:0]    io_in_2_bits_head_eth_type    =0;
reg       [31:0]    io_in_2_bits_head_next_idx    =0;
reg       [31:0]    io_in_2_bits_head_bitmap      =0;
reg       [31:0]    io_in_2_bits_head_index       =0;
reg                 io_in_2_bits_is_empty         =0;
wire                io_in_3_ready                 ;
reg                 io_in_3_valid                 =0;
reg       [15:0]    io_in_3_bits_head_eth_type    =0;
reg       [31:0]    io_in_3_bits_head_next_idx    =0;
reg       [31:0]    io_in_3_bits_head_bitmap      =0;
reg       [31:0]    io_in_3_bits_head_index       =0;
reg                 io_in_3_bits_is_empty         =0;
reg                 io_out_ready                  =0;
wire                io_out_valid                  ;
wire      [15:0]    io_out_bits_head_eth_type     ;
wire      [31:0]    io_out_bits_head_next_idx     ;
wire      [31:0]    io_out_bits_head_bitmap       ;
wire      [31:0]    io_out_bits_head_index        ;
wire                io_out_bits_is_empty          ;
wire                io_idx_ready                  ;
reg                 io_idx_valid                  =0;
reg       [1:0]     io_idx_bits                   =0;

IN#(113)in_io_in_0(
        clock,
        reset,
        {io_in_0_bits_head_eth_type,io_in_0_bits_head_next_idx,io_in_0_bits_head_bitmap,io_in_0_bits_head_index,io_in_0_bits_is_empty},
        io_in_0_valid,
        io_in_0_ready
);
// head_eth_type, head_next_idx, head_bitmap, head_index, is_empty
// 16'h0, 32'h0, 32'h0, 32'h0, 1'h0

IN#(113)in_io_in_1(
        clock,
        reset,
        {io_in_1_bits_head_eth_type,io_in_1_bits_head_next_idx,io_in_1_bits_head_bitmap,io_in_1_bits_head_index,io_in_1_bits_is_empty},
        io_in_1_valid,
        io_in_1_ready
);
// head_eth_type, head_next_idx, head_bitmap, head_index, is_empty
// 16'h0, 32'h0, 32'h0, 32'h0, 1'h0

IN#(113)in_io_in_2(
        clock,
        reset,
        {io_in_2_bits_head_eth_type,io_in_2_bits_head_next_idx,io_in_2_bits_head_bitmap,io_in_2_bits_head_index,io_in_2_bits_is_empty},
        io_in_2_valid,
        io_in_2_ready
);
// head_eth_type, head_next_idx, head_bitmap, head_index, is_empty
// 16'h0, 32'h0, 32'h0, 32'h0, 1'h0

IN#(113)in_io_in_3(
        clock,
        reset,
        {io_in_3_bits_head_eth_type,io_in_3_bits_head_next_idx,io_in_3_bits_head_bitmap,io_in_3_bits_head_index,io_in_3_bits_is_empty},
        io_in_3_valid,
        io_in_3_ready
);
// head_eth_type, head_next_idx, head_bitmap, head_index, is_empty
// 16'h0, 32'h0, 32'h0, 32'h0, 1'h0

OUT#(113)out_io_out(
        clock,
        reset,
        {io_out_bits_head_eth_type,io_out_bits_head_next_idx,io_out_bits_head_bitmap,io_out_bits_head_index,io_out_bits_is_empty},
        io_out_valid,
        io_out_ready
);
// head_eth_type, head_next_idx, head_bitmap, head_index, is_empty
// 16'h0, 32'h0, 32'h0, 32'h0, 1'h0

IN#(2)in_io_idx(
        clock,
        reset,
        {io_idx_bits},
        io_idx_valid,
        io_idx_ready
);
// 
// 2'h0


P4Arbiter P4Arbiter_inst(
        .*
);


initial begin
        reset <= 1;
        clock = 1;
        #1000;
        reset <= 0;
        #100;
        out_io_out.start();
        #50;
        in_io_in_0.write({16'h0, 32'h0, 32'h0, 32'h0, 1'h0});
        in_io_in_1.write({16'h1, 32'h0, 32'h0, 32'h0, 1'h1});
        in_io_in_1.write({16'h1, 32'h0, 32'h0, 32'h0, 1'h1});
        in_io_in_1.write({16'h1, 32'h0, 32'h0, 32'h0, 1'h0});
        in_io_in_1.write({16'h1, 32'h0, 32'h0, 32'h0, 1'h1});
        in_io_in_0.write({16'h0, 32'h0, 32'h0, 32'h0, 1'h1});
        in_io_in_3.write({16'h3, 32'h0, 32'h0, 32'h0, 1'h1});
        in_io_in_2.write({16'h2, 32'h0, 32'h0, 32'h0, 1'h0});
        in_io_in_3.write({16'h3, 32'h0, 32'h0, 32'h0, 1'h1});
        in_io_in_1.write({16'h1, 32'h0, 32'h0, 32'h0, 1'h1});
        in_io_in_2.write({16'h2, 32'h0, 32'h0, 32'h0, 1'h1});
        in_io_in_3.write({16'h3, 32'h0, 32'h0, 32'h0, 1'h0});
        in_io_in_2.write({16'h2, 32'h0, 32'h0, 32'h0, 1'h1});
        in_io_in_2.write({16'h2, 32'h0, 32'h0, 32'h0, 1'h1});
        in_io_in_0.write({16'h0, 32'h0, 32'h0, 32'h0, 1'h0});
        #10
        in_io_idx.write(2'h0);
        in_io_idx.write(2'h2);
        in_io_idx.write(2'h1);
        in_io_idx.write(2'h3);
        in_io_idx.write(2'h0);

end
always #5 clock=~clock;

endmodule