	ghrd_10as066n2_pr_region_controller_0 u0 (
		.avl_csr_read                   (_connected_to_avl_csr_read_),                   //   input,   width = 1,        avl_csr.read
		.avl_csr_write                  (_connected_to_avl_csr_write_),                  //   input,   width = 1,               .write
		.avl_csr_address                (_connected_to_avl_csr_address_),                //   input,   width = 2,               .address
		.avl_csr_writedata              (_connected_to_avl_csr_writedata_),              //   input,  width = 32,               .writedata
		.avl_csr_readdata               (_connected_to_avl_csr_readdata_),               //  output,  width = 32,               .readdata
		.bridge_freeze0_freeze          (_connected_to_bridge_freeze0_freeze_),          //  output,   width = 1, bridge_freeze0.freeze
		.bridge_freeze0_illegal_request (_connected_to_bridge_freeze0_illegal_request_), //   input,   width = 1,               .illegal_request
		.bridge_freeze1_freeze          (_connected_to_bridge_freeze1_freeze_),          //  output,   width = 1, bridge_freeze1.freeze
		.bridge_freeze1_illegal_request (_connected_to_bridge_freeze1_illegal_request_), //   input,   width = 1,               .illegal_request
		.clock_clk                      (_connected_to_clock_clk_),                      //   input,   width = 1,          clock.clk
		.pr_handshake_start_req         (_connected_to_pr_handshake_start_req_),         //  output,   width = 1,   pr_handshake.start_req
		.pr_handshake_start_ack         (_connected_to_pr_handshake_start_ack_),         //   input,   width = 1,               .start_ack
		.pr_handshake_stop_req          (_connected_to_pr_handshake_stop_req_),          //  output,   width = 1,               .stop_req
		.pr_handshake_stop_ack          (_connected_to_pr_handshake_stop_ack_),          //   input,   width = 1,               .stop_ack
		.reset_reset                    (_connected_to_reset_reset_),                    //   input,   width = 1,          reset.reset
		.reset_source_reset             (_connected_to_reset_source_reset_)              //  output,   width = 1,   reset_source.reset
	);

