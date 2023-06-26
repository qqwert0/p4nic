package p4nic

import chisel3._
import chisel3.util._
import common.axi._
import chisel3.experimental.ChiselEnum
import common.storage._
import qdma._
import common._



class P4nicSpase extends Module {
    val io = IO(new Bundle {
        // Net data
		val c2h_cmd			= Vec(2,Decoupled(new C2H_CMD))
		val c2h_data	    = Vec(2,Decoupled(new C2H_DATA))
		val h2c_cmd			= Vec(2,Decoupled(new H2C_CMD))
		val h2c_data	    = Vec(2,Flipped(Decoupled(new H2C_DATA)))
		val controlReg		= Vec(2,Input(Vec(64,UInt(32.W))))
        val statusReg       = Output(Vec(128,UInt(32.W)))

    })

	val controlReg = io.controlReg
	// val statusReg = Reg(Vec(128,UInt(32.W)))
	ToZero(io.statusReg)


    // Packet gen module
    
    val p4Sim	= (Module(new P4Sim()))

    val packetGen	= Seq.fill(2)(Module(new PacketGenerator()))

	p4Sim.io.NetRx(0)                   <>Delay(packetGen(0).io.cmacTx,200)
    packetGen(0).io.cmacRx              <>Delay(p4Sim.io.NetTx(0),200)
    p4Sim.io.NetRx(1)                   <>Delay(packetGen(1).io.cmacTx,200)
    packetGen(1).io.cmacRx              <>Delay(p4Sim.io.NetTx(1),200)

    for(i<- 0 until 2){
    // Init values
        packetGen(i).io.sAxib.ar.ready	:= 1.U
        packetGen(i).io.sAxib.aw.ready	:= 1.U
        packetGen(i).io.sAxib.w.ready	:= 1.U
        packetGen(i).io.sAxib.r.valid	:= 1.U
        ToZero(packetGen(i).io.sAxib.r.bits)
        packetGen(i).io.sAxib.b.valid	:= 1.U
        ToZero(packetGen(i).io.sAxib.b.bits)	

        packetGen(i).io.h2cData	<> RegSlice(6)(io.h2c_data(i))
        RegSlice(6)(packetGen(i).io.c2hCmd)		<> io.c2h_cmd(i)
        RegSlice(6)(packetGen(i).io.c2hData)	    <> io.c2h_data(i)
        RegSlice(6)(packetGen(i).io.h2cCmd)		<> io.h2c_cmd(i)


        /* For worker, QDMA regs are used as below:
        *
        * Reg(28-29): Param phys address
        * Reg(30)    : Param len
        * Reg(32-33): Param req callback
        * Reg(34)    : Param req valid
        * Reg(36-37): Grad phys address
        * Reg(38)    : Grad len
        * Reg(40-41): Grad req callback
        * Reg(42)    : Grad req valid
        * Reg(60)    : Node rank
        * Reg(640)    : Param req ready
        * Reg(641)    : Grad req ready
        */
        packetGen(i).io.paramReq.bits.addr     := Cat(Seq(controlReg(i)(29), controlReg(i)(28)))
        packetGen(i).io.paramReq.bits.size     := controlReg(i)(30)
        packetGen(i).io.paramReq.bits.callback := Cat(Seq(controlReg(i)(33), controlReg(i)(32)))
        packetGen(i).io.gradReq.bits.addr      := Cat(Seq(controlReg(i)(37), controlReg(i)(36)))
        packetGen(i).io.gradReq.bits.size      := controlReg(i)(38)
        packetGen(i).io.gradReq.bits.callback  := Cat(Seq(controlReg(i)(41), controlReg(i)(40)))
        packetGen(i).io.paramReq.valid         := (controlReg(i)(34)(0) === 1.U && RegNext(controlReg(i)(34)(0)) =/= 1.U)
        
        packetGen(i).io.gradReq.valid          := (controlReg(i)(42)(0) === 1.U && RegNext(controlReg(i)(42)(0)) =/= 1.U)
        packetGen(i).io.rxIdxInitAddr     := controlReg(i)(58)
        packetGen(i).io.rxDataInitAddr     := controlReg(i)(59)
        packetGen(i).io.idxTotalLen     := controlReg(i)(60)
        packetGen(i).io.nodeRank        := controlReg(i)(61)
        packetGen(i).io.engineRand      := controlReg(i)(62)
        packetGen(i).io.RxIdxDepth      := controlReg(i)(63)
        packetGen(i).io.dataTotalLen     := controlReg(i)(56)
        packetGen(i).io.IdxTransNum      := controlReg(i)(57)

        packetGen(i).io.token_small      := controlReg(i)(20)
        packetGen(i).io.token_big       := controlReg(i)(21)


    }
    // statusReg(128) := packetGen.io.paramReq.ready
    // statusReg(129) := packetGen.io.gradReq.ready


    Collector.connect_to_status_reg(io.statusReg, 0)


}