module testbench_P4Sim(

    );

reg                 clock                         =0;
reg                 reset                         =0;
reg                 io_NetTx_0_ready              =0;
wire                io_NetTx_0_valid              ;
wire                io_NetTx_0_bits_last          ;
wire      [511:0]   io_NetTx_0_bits_data          ;
wire      [63:0]    io_NetTx_0_bits_keep          ;
reg                 io_NetTx_1_ready              =0;
wire                io_NetTx_1_valid              ;
wire                io_NetTx_1_bits_last          ;
wire      [511:0]   io_NetTx_1_bits_data          ;
wire      [63:0]    io_NetTx_1_bits_keep          ;
wire                io_NetRx_0_ready              ;
reg                 io_NetRx_0_valid              =0;
reg                 io_NetRx_0_bits_last          =0;
reg       [511:0]   io_NetRx_0_bits_data          =0;
reg       [63:0]    io_NetRx_0_bits_keep          =0;
wire                io_NetRx_1_ready              ;
reg                 io_NetRx_1_valid              =0;
reg                 io_NetRx_1_bits_last          =0;
reg       [511:0]   io_NetRx_1_bits_data          =0;
reg       [63:0]    io_NetRx_1_bits_keep          =0;

OUT#(577)out_io_NetTx_0(
        clock,
        reset,
        {io_NetTx_0_bits_last,io_NetTx_0_bits_data,io_NetTx_0_bits_keep},
        io_NetTx_0_valid,
        io_NetTx_0_ready
);
// last, data, keep
// 1'h0, 512'h0, 64'h0

OUT#(577)out_io_NetTx_1(
        clock,
        reset,
        {io_NetTx_1_bits_last,io_NetTx_1_bits_data,io_NetTx_1_bits_keep},
        io_NetTx_1_valid,
        io_NetTx_1_ready
);
// last, data, keep
// 1'h0, 512'h0, 64'h0

IN#(577)in_io_NetRx_0(
        clock,
        reset,
        {io_NetRx_0_bits_last,io_NetRx_0_bits_data,io_NetRx_0_bits_keep},
        io_NetRx_0_valid,
        io_NetRx_0_ready
);
// last, data, keep
// 1'h0, 512'h0, 64'h0

IN#(577)in_io_NetRx_1(
        clock,
        reset,
        {io_NetRx_1_bits_last,io_NetRx_1_bits_data,io_NetRx_1_bits_keep},
        io_NetRx_1_valid,
        io_NetRx_1_ready
);
// last, data, keep
// 1'h0, 512'h0, 64'h0


P4Sim P4Sim_inst(
        .*
);


initial begin
        reset <= 1;
        clock = 1;
        #1000;
        reset <= 0;
        #100;
        out_io_NetTx_0.start();
        out_io_NetTx_1.start();
        #50;
        in_io_NetRx_0.write({1'h0,336'h2,176'h0,64'hffff_ffff_ffff_ffff});
        in_io_NetRx_1.write({1'h0,336'h3,176'h0,64'hffff_ffff_ffff_ffff});
        in_io_NetRx_0.write({1'h0,336'h2,176'h1,64'hffff_ffff_ffff_ffff});
        in_io_NetRx_1.write({1'h0,336'h2,176'h1,64'hffff_ffff_ffff_ffff});
        in_io_NetRx_0.write({1'h1,336'h2,176'h1,64'hffff_ffff_ffff_ffff});
        in_io_NetRx_1.write({1'h1,336'h2,176'h1,64'hffff_ffff_ffff_ffff}); 
        in_io_NetRx_0.write({1'h1,336'h5,176'h0,64'hffff_ffff_ffff_ffff});
        in_io_NetRx_1.write({1'h0,336'h3,176'h0,64'hffff_ffff_ffff_ffff});        
        in_io_NetRx_1.write({1'h0,336'h3,176'h0,64'hffff_ffff_ffff_ffff});   
        in_io_NetRx_1.write({1'h1,336'h3,176'h0,64'hffff_ffff_ffff_ffff});        
end
always #5 clock=~clock;

endmodule