# How to add a New Interface 

This guide will teach you add a new interface to the SpatialIP Module, which is useful if you want to do things like 
support new interfaces (i.e. optical ports) in Spatial for your backend.

You probably want to do something like:
```
val in = StreamIn[T](someBus)
val out = StreamOut[V](someOtherBus)
Accel{
    Stream {
        ...
        ... = in.value() // Deq one element when valid
        ...
        out := ... // Enq one element when ready
    } 
}
```

# Things You Will Need to Edit
To add a new stream interface, you will need to edit the following places
* spatial/src/spatial/lang/ - This is where you define the syntax/API for your new stream.  A new StreamIn
requires a Bits type [T] as well as a Bus case class.  The type [T] tells you what data type to expect when you
deq/enq in the Accel, and the Bus helps codegen/Fringe wire this interface to the correct pins on the SpatialIP wrapper.
The StreamIn/Out[T] gives you a Decoupled(T) in Chisel-speak, meaning you have a bus for the width of T and ready/valid
signals that go along with it.  The ready/valid signals are used in the control flow for Spatial Stream controllers.
You can use the Bus case class info in codegen if you actually need to handle more than a DecoupledIO, or you can make the type
a Tuple if you need to manipulate more data in the Spatial app (i.e. ARUSER, AWID, etc.) 
    * Bus.scala - This is where Bus case classes are defined. 
* spatial/src/spatial/codegen/chiselgen - This is where you set up some wires for your stream to interact with Fringe (if you 
need some parameterized hardware like the load/store/scatter/gather DMA) or you can hook up to the SpatialIP interface directly
    * ChiselGenStream.scala - You need to add the following:
        * StreamInNew/StreamOutNew rules - If your StreamIn/Out will be defined outside the Accel (i.e. val in = StreamIn[T](...) as
        opposed to an implicit StreamIn[CmdBus](BurstCmdBus()) that is created inside the Accel by a BlockBox lowering rule), and you
        the IR will contain a node for this StreamInNew/OutNew *outside* of the Accel scope, and will refer to this
        IR node later in the accel.  You should match on your Bus and assign $lhs to that DecoupledIO port.  For StreamIn[T](bus),
        the T tells you the type to expect when you interact with it, the bus tells you what interface (i.e. AXI Stream) the data is
        coming over.
        Spatial's modularizer and Java chunker will handle the boilerplate so that anywhere in your app that requires this
        Stream will have access to this $lhs
        * StreamOutBankedWrite/StreamInBankedRead rules - This is how you extract data off of your Bus. The unroller turns all
        accesses into BankedAccesses, meaning that the data is a Seq[T]. This is useful for parallelized loads/stores, but for most
        cases you probably just have a Seq of size 1.
        * streamIns/streamOuts Lists - TODO. It may be useful to capture a list of the streams you encounter during codegen
        so that you can emit something in the Accel top-level files (during emitPostMain or emitFooter) that gives you parameters for the streams.
    * ChiselCodegen.scala - Rules for quoting the specific type for your StreamIn/Out Bus.  This should be pretty straightforward.
    You have to define these rules so that the modularizer/chunker knows how to pass the Streams around. You may choose to 
    just leave the default case for your stream.
* spatial/fringe/src/fringe - This is where all the chisel stuff goes to support your new stream as an interface for the AccelUnit as well as the SpatialIP
    * AccelTopInterface.scala - You will probably need to add your interface to the accel io, if you can't piggy back on one of the
    existing streams.  Your Accel wrapper will have as many streams as you specify.  As of now, only CXP and KCU1500 targets 
    have one in and one out AxiStream on their SpatialIP wrapper
    * targets/\<your target device\>/\<your target device\>Interface.scala - Add wires for you stream to connect to the outside world
    on the SpatialIP wrapper definition
    * targets/\<your target device\>/\<your target device\>.scala - Define rules for connecting the interface to your stream
    that exists on the Accel wrapper to the interface on the SpatialIP
    
    
    

# Additional Steps for New DRAM-like Node
If you need to add something that is DRAM like (with load/store/scatter/gather syntax and various streams involved), 
you also need to do a few extra things.  One example of this is the Frame stuff that was added to support 
SLAC's Rogue paradigm
1) Add the API and relevent nodes in src/spatial/lang and src/spatial/node.  The "<object> store <mem>" function is defined in
src/lang/types/Mem.scala
2) Edit src/spatial/node/\<something\>.scala and add a lowering rule for the transfer nodes.  This usually imports spatial dsl and uses regular spatial syntax.
3) Remember to register the lowering rule in src/spatial/transform/BlackboxLowering.scala