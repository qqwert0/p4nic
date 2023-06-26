`timescale 1ns / 1ps
//////////////////////////////////////////////////////////////////////////////////
// Company: 
// Engineer: 
// 
// Create Date: 10/27/2021 10:37:21 AM
// Design Name: 
// Module Name: TESTER
// Project Name: 
// Target Devices: 
// Tool Versions: 
// Description: 
// 
// Dependencies: 
// 
// Revision:
// Revision 0.01 - File Created
// Additional Comments:
// 
//////////////////////////////////////////////////////////////////////////////////


module IN #(int width = 64)(
		input wire clock,
		input wire reset,
		output reg [width-1:0]data,
		output reg valid,
		input wire ready
    );
	longint wr_index=0;
	longint rd_index=0;
	bit [10000:0][width-1:0]fifo;

	always @(posedge clock) begin
		if(reset)begin
			wr_index=0;
			rd_index=0;
			fifo=0;
		end
        else begin
            if(valid & ready)begin
                rd_index=rd_index+1;
                // $display("fire\n");
            end
            data = fifo[rd_index];
            if(wr_index>rd_index)begin
                valid=1;
            end 
            else begin
                valid=0;
            end            
        end
	end
	function write(bit[width-1:0] value);
		fifo[wr_index] = value;
		wr_index = wr_index+1;
	endfunction

endmodule

module OUT #(int width)(
		input wire clock,
		input wire reset,
		input wire [width-1:0]data,
		input wire valid,
		output reg ready
    );
	longint wr_index=0;
	longint rd_index=0;
	bit [10000:0][width-1:0]fifo;

	always @(posedge clock) begin
		if(reset)begin
			wr_index=0;
			rd_index=0;
			fifo=0;
		end
        else begin
            if(valid & ready)begin
                fifo[wr_index] = data;
                wr_index = wr_index+1;
            end            
        end
	end

	function start();
		ready = 1;
	endfunction

	function stop();
		ready = 0;
	endfunction

	task hope(bit[width-1:0] value, int strict=0);
		if(strict && (wr_index==rd_index))begin
			$display("No data expected, start waiting");
		end
		wait(wr_index>rd_index);
		if(value == fifo[rd_index])begin
			$display("PASS, %h",value);
		end
		else begin
			$display("ERROR, expected: %h, actual :%h",value,fifo[rd_index]);
		end
		rd_index = rd_index+1;
	endtask


endmodule


    module DMA #(int width = 512,int READ_DELAY = 1,int WRITE_IDLE = 1)(
		input wire          clock,
		input wire          reset,
        //DMA CMD streams
        input wire                  read_cmd_valid,
        output wire                 read_cmd_ready,
        input wire [63:0]           read_cmd_address,
        input wire [31:0]           read_cmd_length,
        input wire                  write_cmd_valid,
        output wire                 write_cmd_ready,
        input wire [63:0]           write_cmd_address,
        input wire [31:0]           write_cmd_length,        
        //DMA Data streams      
        output wire                 read_data_valid,
        input wire                  read_data_ready,
        output reg [width-1:0]      read_data_data,
        output reg [(width/8)-1:0]  read_data_keep,
        output wire                 read_data_last,
        input wire                  write_data_valid,
        output wire                 write_data_ready,
        input wire [width-1:0]      write_data_data,
        input reg [(width/8)-1:0]   write_data_keep,
        input wire                  write_data_last        
    );
	int rd_cmd_rd_index=0;
	int rd_cmd_wr_index=0;
    int rd_cmd_count=0;
    int rd_delay_count,rd_idle_count,rd_busy_count;
    int wr_delay_count,wr_busy_count;
	parameter width_bits = $clog2(width) - 3;
    integer i;

    bit [511:0][95:0]   rd_cmd_fifo,wr_cmd_fifo;
	bit [1048576:0][width-1:0]fifo;
    
    localparam [3:0]    IDLE = 4'd0,
                        READ_DATA = 4'd1,
                        WRITE_DATA = 4'd2,
                        READ_IDLE = 4'd3,
                        DELAY = 4'd4;

    reg [3:0]           rd_data_state;
    reg [31:0]          read_addr,read_length;

    assign read_cmd_ready = rd_cmd_count < 512;
    assign read_data_valid = rd_data_state == READ_DATA;
    assign read_data_last = (read_data_ready & read_data_valid) && (read_length==1);
    assign read_data_keep = 64'hffff_ffff_ffff_ffff;

	always @(posedge clock) begin
		if(reset)begin
			rd_cmd_wr_index <=0;
			rd_cmd_rd_index <=0;
			rd_cmd_fifo     <=0;
		end
        else begin
            if(read_cmd_valid & read_cmd_ready)begin
                rd_cmd_wr_index                 <= rd_cmd_wr_index + 1;
                rd_cmd_fifo[rd_cmd_wr_index]    <= {read_cmd_address,read_cmd_length};
                if(rd_cmd_wr_index == 511)begin
                    rd_cmd_wr_index             <= 0;
                end
            end

            if((rd_data_state == IDLE) & (rd_cmd_count>0))begin
                rd_cmd_rd_index                 <= rd_cmd_rd_index + 1;
                if(rd_cmd_rd_index == 511)begin
                    rd_cmd_rd_index             <= 0;
                end
            end
        end
	end

	always @(posedge clock) begin
		if(reset)begin
			rd_cmd_count <=0;
		end
        else begin
            if(read_cmd_valid & read_cmd_ready && (!((rd_data_state == IDLE) & (rd_cmd_count>0))))begin
                rd_cmd_count                 <= rd_cmd_count + 1;
            end
            else if ((rd_data_state == IDLE) & (rd_cmd_count>0) & (!(read_cmd_valid & read_cmd_ready)))begin
                rd_cmd_count                <= rd_cmd_count - 1;
            end
        end
	end

    always@(posedge clock)begin
		if(reset)begin
			rd_delay_count                  <=0;
		end
        else if(rd_data_state == DELAY)begin
            rd_delay_count                  <= rd_delay_count + 1;
        end        
        else begin
            rd_delay_count                  <= 0;
        end
    end

    always@(posedge clock)begin
		if(reset)begin
			rd_busy_count                  <=0;
		end
        else if(rd_data_state == READ_DATA)begin
            rd_busy_count                  <= rd_busy_count + 1;
        end        
        else begin
            rd_busy_count                  <= 0;
        end
    end


    always@(posedge clock)begin
		if(reset)begin
			rd_idle_count                  <=0;
		end
        else if(rd_data_state == READ_IDLE)begin
            rd_idle_count                  <= rd_idle_count + 1;
        end        
        else begin
            rd_idle_count                  <= 0;
        end
    end


    always@(posedge clock)begin
        if(reset)begin
			rd_data_state       <= IDLE;
            read_addr           <= 0;
            read_length         <= 0;
            read_data_data      <= 0;
		end
        else begin
            case(rd_data_state)
                IDLE:begin
                    if(rd_cmd_count>0)begin
                        rd_data_state       <= DELAY;
                        read_addr           <= rd_cmd_fifo[rd_cmd_rd_index][62:(width_bits+32)];
                        read_length         <= rd_cmd_fifo[rd_cmd_rd_index][31:width_bits];
                        read_data_data      <= fifo[rd_cmd_fifo[rd_cmd_rd_index][62:(width_bits+32)]];
                    end
                    else begin
                        rd_data_state       <= IDLE;
                    end
                end
                DELAY:begin
                    read_data_data      <= fifo[read_addr];
                    if(rd_delay_count == READ_DELAY)begin
                        rd_data_state       <= READ_DATA;
                    end
                    else begin
                        rd_data_state       <= DELAY;
                    end
                end
                READ_DATA:begin
                    if(read_data_ready & read_data_valid)begin
                        read_length         <= read_length - 1;
                        read_addr           <= read_addr + 1;
                        read_data_data      <= fifo[read_addr+1];
                        if(read_length == 1)begin
                            rd_data_state   <= IDLE;
                        end
                        else if(rd_busy_count == 8)begin
                            rd_data_state   <= READ_IDLE;
                        end
                        else begin
                            rd_data_state   <= READ_DATA;
                        end
                    end
                end
                READ_IDLE:begin
                    read_data_data      <= fifo[read_addr];
                    if(rd_idle_count == 1)begin
                        rd_data_state       <= READ_DATA;
                    end
                    else begin
                        rd_data_state       <= READ_IDLE;
                    end
                end

            endcase
        end
    end

	longint wr_cmd_rd_index=0;
	longint wr_cmd_wr_index=0;
    longint wr_cmd_count=0;

    reg [3:0]           wr_data_state;
    reg [31:0]          write_addr,write_length;
    
    assign write_cmd_ready = wr_cmd_count < 512;
    assign write_data_ready = wr_data_state == WRITE_DATA;

	always @(posedge clock) begin
		if(reset)begin
			wr_cmd_wr_index <=0;
			wr_cmd_rd_index <=0;
			wr_cmd_fifo     <=0;
		end
        else begin
            if(write_cmd_valid & write_cmd_ready)begin
                wr_cmd_wr_index                 <= wr_cmd_wr_index + 1;
                wr_cmd_fifo[wr_cmd_wr_index]    <= {write_cmd_address,write_cmd_length};
                if(wr_cmd_wr_index == 511)begin
                    wr_cmd_wr_index             <= 0;
                end
            end

            if((wr_data_state == IDLE) & (wr_cmd_count>0))begin
                wr_cmd_rd_index                 <= wr_cmd_rd_index + 1;
                if(wr_cmd_rd_index == 511)begin
                    wr_cmd_rd_index             <= 0;
                end
            end
        end
	end

	always @(posedge clock) begin
		if(reset)begin
			wr_cmd_count <=0;
		end
        else begin
            if(write_cmd_valid & write_cmd_ready && (!((wr_data_state == IDLE) & (wr_cmd_count>0))))begin
                wr_cmd_count                 <= wr_cmd_count + 1;
            end
            else if ((wr_data_state == IDLE) & (wr_cmd_count>0) & (!(write_cmd_valid & write_cmd_ready)))begin
                wr_cmd_count                <= wr_cmd_count - 1;
            end
        end
	end

    always@(posedge clock)begin
		if(reset)begin
			wr_delay_count                  <=0;
		end
        else if(wr_data_state == DELAY)begin
            wr_delay_count                  <= wr_delay_count + 1;
        end        
        else begin
            wr_delay_count                  <= 0;
        end
    end

    always@(posedge clock)begin
		if(reset)begin
			wr_busy_count                  <=0;
		end
        else if(wr_data_state == WRITE_DATA)begin
            wr_busy_count                  <= wr_busy_count + 1;
        end        
        else begin
            wr_busy_count                  <= 0;
        end
    end


    genvar j;

    always@(posedge clock)begin
        if(reset)begin
			wr_data_state       <= IDLE;
            write_addr          <= 0;
            write_length        <= 0;
		end
        else begin
            case(wr_data_state)
                IDLE:begin
                    if(wr_cmd_count>0)begin
                        wr_data_state       <= WRITE_DATA;
						write_addr          <= wr_cmd_fifo[wr_cmd_rd_index][62:(width_bits+32)];
                        write_length        <= wr_cmd_fifo[wr_cmd_rd_index][31:width_bits];
                    end
                end
                WRITE_DATA:begin
                    if(write_data_ready & write_data_valid)begin
                        write_length         <= write_length - 1;
                        write_addr           <= write_addr + 1;
                        if(write_length == 1)begin
                            wr_data_state   <= IDLE;
                        end
                        else if(wr_busy_count == 5)begin
                            wr_data_state   <= DELAY;
                        end
                        else begin
                            wr_data_state   <= WRITE_DATA;
                        end
                    end
                end
                DELAY:begin
                    if(wr_delay_count == WRITE_IDLE)begin
                        wr_data_state       <= WRITE_DATA;
                    end
                    else begin
                        wr_data_state       <= DELAY;
                    end
                end
            endcase
        end
    end

    generate
        for(j = 0; j < width/8; j = j + 1) begin
            always@(posedge clock)begin
                if(reset)begin
                    fifo[write_addr][j*8+7:j*8] <= 0;
                end
                else if(wr_data_state == WRITE_DATA & write_data_ready & write_data_valid)begin
                    fifo[write_addr][j*8+7:j*8] <=  write_data_keep[j]?  write_data_data[j*8+7:j*8] : fifo[write_addr][j*8+7:j*8];
                end
            end
        end
    endgenerate





    task init_incr(int start_addr, int length, int offset);//单位 cacheword
        start_addr = start_addr/64;
        length     = length/64;
        for(i=0;i<length;i=i+1)begin
            fifo[start_addr+i] = i+offset;
        end
    endtask

    task check_mem(int start_addr, int length, int offset);//单位 cacheword
        start_addr = start_addr/64;
        length     = length/64;
        for(i=0;i<length;i++)begin
            if(fifo[start_addr+i] != (i+offset))begin
                $display("check error");
                $display("at %d memory, correct value is %x, real value is %d", start_addr+i, i+offset, fifo[start_addr+i]);
                $finish;
            end
        end
        $display("check success");
    endtask

	task init_from_file(string path,integer line);
		integer data_file;
		data_file = $fopen(path, "r");
		if (data_file == 0) begin
			$display("data_file handle was NULL");
			$finish;
		end
		for(i=0;i<line;i++)begin
			$fscanf(data_file,"%x" ,fifo[i]) ;
			// $display("%x\n",fifo[i]);
		end

	endtask

endmodule






