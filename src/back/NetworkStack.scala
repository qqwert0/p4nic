package p4nic

import chisel3._
import chisel3.util._
import common.axi._
import chisel3.experimental.ChiselEnum
import common.storage._
import qdma._
import common._

/*
class PacketRequest extends Bundle {
    val addr     = Output(UInt(64.W))
    val size     = Output(UInt(32.W))
    val callback = Output(UInt(64.W))
}
*/

class NetworkStack extends Module {
    val io = IO(new Bundle {
        // Net data
        val NetTx      = (Decoupled(new AXIS(512)))
        val NetRx      = Flipped(Decoupled(new AXIS(512)))
        // User data
        val DataTx      = Flipped(Decoupled(UInt(512.W)))
        val DataRx      = (Decoupled(UInt(512.W)))
        //write cmd
        val wrMemCmd    = (Decoupled(new PacketRequest))  //addr,  size,   callback=0

        //config
        val nodeRank    = Input(UInt(32.W))
    })


    ToZero(io.wrMemCmd.valid)
    ToZero(io.wrMemCmd.bits)
    //TX
    val tx_pkg_gen = Module(new NetTxPkg())
    val eth_lshift = Module(new LSHIFT(14,512)) 
    val tx_add_eth = Module(new NetTxAddEth())

    //RX
    val eth_rshift = Module(new RSHIFT(14,512))

    //TX
    tx_pkg_gen.io.DataTx        <>  io.DataTx
    eth_lshift.io.in            <>  tx_pkg_gen.io.NetTx
    tx_add_eth.io.DataIn        <>  eth_lshift.io.out
    io.NetTx                    <>  tx_add_eth.io.DataOut

    //RX
    eth_rshift.io.in            <>  io.NetRx
    io.DataRx.bits              :=  eth_rshift.io.out.bits.data
    io.DataRx.valid             :=  eth_rshift.io.out.valid
    eth_rshift.io.out.ready     :=  io.DataRx.ready

    dontTouch(io.wrMemCmd)




}