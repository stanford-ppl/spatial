library IEEE;
    use IEEE.std_logic_1164.all;
    use IEEE.numeric_std.all;
    use IEEE.math_real.all;

LIBRARY altera_mf;

-- ---------------------
   Entity ICON_GEN_ST is
-- ---------------------
   generic ( NB_BITS_SYMB : natural:=10 );
   port ( VID_RST     : in  std_logic                                        -- Pixel Clock
        ; VID_CLK     : in  std_logic                                        -- Asynchronous Reset
        -- Avalon Video Streaming Video
        ; VID_RDY     : in  std_logic                                        -- Input Ready signal
        ; VID_SOP     : out std_logic                                        -- Start of Packet
        ; VID_DAV     : out std_logic                                        -- Data Valid
        ; VID_DATA    : out std_logic_vector((NB_BITS_SYMB*3*2)-1 downto 0)  -- Video Data
        ; VID_EOP     : out std_logic                                        -- End of Packet
        );
-- ------------------------
   End entity ICON_GEN_ST;
-- ------------------------

-- -----------------------------------
   Architecture RTL of ICON_GEN_ST is
-- -----------------------------------

  constant OUTPUT_REG : boolean:=true;

  constant BLANK_DUR : natural:=5; -- blanking used

  constant NB_PIX_CLK : natural:=2;

  -- Nibble size : 4 bits (used to describe the Control Packet content)
  constant NIBBLE_SIZE : natural:=4;

  -- Initial resolution : 160 x 90
  --constant LOGO_SIZE_X : natural:=160;
  --constant LOGO_SIZE_Y : natural:=90;
  constant LOGO_SIZE_X : natural:=306;
  constant LOGO_SIZE_Y : natural:=191;

  constant iWIDTH  : std_logic_vector(15 downto 0):=std_logic_vector(to_unsigned(LOGO_SIZE_X,16));
  constant iHEIGHT : std_logic_vector(15 downto 0):=std_logic_vector(to_unsigned(LOGO_SIZE_Y,16));
  constant iTYPE   : std_logic_vector(3 downto 0):=x"3";  -- Progressive Image

  component altsyncram
      generic (
          operation_mode                 : string := "BIDIR_DUAL_PORT";
          -- port a parameters
          width_a                        : integer := 1;
          widthad_a                      : integer := 1;
          numwords_a                     : integer := 0;
          -- registering parameters
          -- port a read parameters
          outdata_reg_a                  : string := "UNREGISTERED";
          -- clearing parameters
          address_aclr_a                 : string := "NONE";
          outdata_aclr_a                 : string := "NONE";
          -- clearing parameters
          -- port a write parameters
          indata_aclr_a                  : string := "NONE";
          wrcontrol_aclr_a               : string := "NONE";
          -- clear for the byte enable port reigsters which are clocked by clk0
          byteena_aclr_a                 : string := "NONE";
          -- width of the byte enable ports. if it is used, must be WIDTH_WRITE_A/8 or /9
          width_byteena_a                : integer := 1;
          -- port b parameters
          width_b                        : integer := 1;
          widthad_b                      : integer := 1;
          numwords_b                     : integer := 0;
          -- registering parameters
          -- port b read parameters
          rdcontrol_reg_b                : string := "CLOCK1";
          address_reg_b                  : string := "CLOCK1";
          outdata_reg_b                  : string := "UNREGISTERED";
          -- clearing parameters
          outdata_aclr_b                 : string := "NONE";
          rdcontrol_aclr_b               : string := "NONE";
          -- registering parameters
          -- port b write parameters
          indata_reg_b                   : string := "CLOCK1";
          wrcontrol_wraddress_reg_b      : string := "CLOCK1";
          -- registering parameter for the byte enable reister for port b
          byteena_reg_b                  : string := "CLOCK1";
          -- clearing parameters
          indata_aclr_b                  : string := "NONE";
          wrcontrol_aclr_b               : string := "NONE";
          address_aclr_b                 : string := "NONE";
          -- clear parameter for byte enable port register
          byteena_aclr_b                 : string := "NONE";
          -- StratixII only : to bypass clock enable or using clock enable
          clock_enable_input_a           : string := "NORMAL";
          clock_enable_output_a          : string := "NORMAL";
          clock_enable_input_b           : string := "NORMAL";
          clock_enable_output_b          : string := "NORMAL";
          -- width of the byte enable ports. if it is used, must be WIDTH_WRITE_A/8 or /9
          width_byteena_b                : integer := 1;
          -- clock enable setting for the core
          clock_enable_core_a            : string := "USE_INPUT_CLKEN";
          clock_enable_core_b            : string := "USE_INPUT_CLKEN";
          -- read-during-write-same-port setting
          read_during_write_mode_port_a  : string := "NEW_DATA_NO_NBE_READ";
          read_during_write_mode_port_b  : string := "NEW_DATA_NO_NBE_READ";
          -- ECC status ports setting
          enable_ecc                     : string := "FALSE";
          ecc_pipeline_stage_enabled     : string := "FALSE";

          width_eccstatus                : integer := 3;
          -- global parameters
          -- width of a byte for byte enables
          byte_size                      : integer := 0;
          read_during_write_mode_mixed_ports: string := "DONT_CARE";
          -- ram block type choices are "AUTO", "M512", "M4K" and "MEGARAM"
          ram_block_type                 : string := "AUTO";
          -- determine whether LE support is turned on or off for altsyncram
          implement_in_les               : string := "OFF";
          -- determine whether RAM would be power up to uninitialized or not
          power_up_uninitialized         : string := "FALSE";

          sim_show_memory_data_in_port_b_layout :  string  := "OFF";

          -- general operation parameters
          init_file                      : string := "UNUSED";
          init_file_layout               : string := "UNUSED";
          maximum_depth                  : integer := 0;
          intended_device_family         : string := "Stratix";
          lpm_hint                       : string := "UNUSED";
          lpm_type                       : string := "altsyncram" );
      port (
          wren_a    : in std_logic := '0';
          wren_b    : in std_logic := '0';
          rden_a    : in std_logic := '1';
          rden_b    : in std_logic := '1';
          data_a    : in std_logic_vector(width_a - 1 downto 0):= (others => '1');
          data_b    : in std_logic_vector(width_b - 1 downto 0):= (others => '1');
          address_a : in std_logic_vector(widthad_a - 1 downto 0);
          address_b : in std_logic_vector(widthad_b - 1 downto 0) := (others => '1');

          clock0    : in std_logic := '1';
          clock1    : in std_logic := 'Z';
          clocken0  : in std_logic := '1';
          clocken1  : in std_logic := '1';
          clocken2  : in std_logic := '1';
          clocken3  : in std_logic := '1';
          aclr0     : in std_logic := '0';
          aclr1     : in std_logic := '0';
          byteena_a : in std_logic_vector( (width_byteena_a - 1) downto 0) := (others => '1');
          byteena_b : in std_logic_vector( (width_byteena_b - 1) downto 0) := (others => 'Z');

          addressstall_a : in std_logic := '0';
          addressstall_b : in std_logic := '0';

          q_a            : out std_logic_vector(width_a - 1 downto 0);
          q_b            : out std_logic_vector(width_b - 1 downto 0);

          eccstatus      : out std_logic_vector(width_eccstatus-1 downto 0) := (others => '0') );
  end component;

  -- named subtypes used of the Output Control Header
  -- Width management
  subtype WIDTH_15_12 is natural range NIBBLE_SIZE + (0*NB_BITS_SYMB) - 1 downto 0*NB_BITS_SYMB;
  subtype WIDTH_11_08 is natural range NIBBLE_SIZE + (1*NB_BITS_SYMB) - 1 downto 1*NB_BITS_SYMB;
  subtype WIDTH_07_04 is natural range NIBBLE_SIZE + (2*NB_BITS_SYMB) - 1 downto 2*NB_BITS_SYMB;
  subtype WIDTH_03_00 is natural range NIBBLE_SIZE + (3*NB_BITS_SYMB) - 1 downto 3*NB_BITS_SYMB;
  -- Height management
  subtype HEIGHT_15_12 is natural range NIBBLE_SIZE + (4*NB_BITS_SYMB) - 1 downto 4*NB_BITS_SYMB;
  subtype HEIGHT_11_08 is natural range NIBBLE_SIZE + (5*NB_BITS_SYMB) - 1 downto 5*NB_BITS_SYMB;
  subtype HEIGHT_07_04 is natural range NIBBLE_SIZE + (0*NB_BITS_SYMB) - 1 downto 0*NB_BITS_SYMB;
  subtype HEIGHT_03_00 is natural range NIBBLE_SIZE + (1*NB_BITS_SYMB) - 1 downto 1*NB_BITS_SYMB;

  subtype CTRL_TYPE is natural range NIBBLE_SIZE + (2*NB_BITS_SYMB) - 1 downto 2*NB_BITS_SYMB;

  Type FSM_t is ( IDLE
                , CTRL_HEADER_ID
                , CTRL_HEADER_END
                , DATA_HEADER
                , SEND_BLANK
                , SEND_LINE
                );
  signal FSM, GOTO : FSM_t;

  constant MIF_FILE_NAME_R : string:="./ip/external_logo_module/rawlogo_r.mif";
  constant MIF_FILE_NAME_G : string:="./ip/external_logo_module/rawlogo_g.mif";
  constant MIF_FILE_NAME_B : string:="./ip/external_logo_module/rawlogo_b.mif";

  signal CNT_X : unsigned(7 downto 0); -- size of lines
  signal CNT_Y : unsigned(7 downto 0); -- number of lines
  signal CNT_BL : natural range 0 to BLANK_DUR-2;

  signal iVID_SOP   : std_logic;
  signal iVID_DAV   : std_logic;
  signal iVID_EOP   : std_logic;
  signal iVID_RDY   : std_logic;
  signal iVID_DATA  : std_logic_vector(VID_DATA'range);
  signal iVID_DATA2 : std_logic_vector(VID_DATA'range);

  signal RAM_ADDR   : std_logic_vector(14 downto 0);
  signal RAM_DATA_R : std_logic_vector(15 downto 0);
  signal RAM_DATA_G : std_logic_vector(15 downto 0);
  signal RAM_DATA_B : std_logic_vector(15 downto 0);


-----\
Begin
-----/

  -- --------------------------------------
  -- 2 Pixels in // managed per clock cycle
  -- --------------------------------------
  -- Image Generation
  GEN_proc : process(VID_RST, VID_CLK)
    variable RAM_PIX    : natural range 0 to LOGO_SIZE_X-1;
    variable RAM_LIN    : natural range 0 to LOGO_SIZE_Y-1;
  begin
    if VID_RST='1' then
      FSM            <= IDLE;
      GOTO           <= IDLE;
      iVID_SOP       <= '0';
      iVID_DAV       <= '0';
      iVID_DATA      <= (others => '0');
      iVID_EOP       <= '0';
      CNT_X          <= (others => '0');
      CNT_Y          <= (others => '0');
      CNT_BL         <=  0 ;
      RAM_ADDR       <= (others => '0');
      RAM_PIX        :=  0 ;
      RAM_LIN        :=  0 ;

    elsif rising_edge(VID_CLK) then

      iVID_SOP <= '0';
      iVID_DAV <= '0';
      iVID_EOP <= '0';

      case FSM is
        -- Wait for the Ready input
        when IDLE =>
          CNT_X          <= (others => '0');
          CNT_Y          <= (others => '0');
          iVID_SOP       <= '0';
          iVID_DAV       <= '0';
          iVID_EOP       <= '0';
          RAM_PIX        :=  0 ;
          RAM_LIN        :=  0 ;
          if iVID_RDY='1' then -- readyLatency=1
            FSM       <= CTRL_HEADER_ID;
            iVID_SOP  <= '1'; -- First word of the Control Packet
            iVID_DAV  <= '1';
            iVID_DATA <= (others => '0');
            iVID_DATA(3 downto 0) <= (others => '1'); -- Packet Type Identifier
          end if;

        -- Header ID
        when CTRL_HEADER_ID =>
          if iVID_RDY='1' then
            FSM                      <= CTRL_HEADER_END;
            iVID_SOP                 <= '0';
            iVID_DAV                 <= '1';
            iVID_EOP                 <= '0';
            iVID_DATA                <= (others => '0');
            -- First Pixel : Width
            iVID_DATA(WIDTH_07_04)   <= iWIDTH((2*NIBBLE_SIZE)-1 downto (1*NIBBLE_SIZE));
            iVID_DATA(WIDTH_11_08)   <= iWIDTH((3*NIBBLE_SIZE)-1 downto (2*NIBBLE_SIZE));
            iVID_DATA(WIDTH_15_12)   <= iWIDTH((4*NIBBLE_SIZE)-1 downto (3*NIBBLE_SIZE));
            -- 2nd Pixel : Width & Height
            iVID_DATA(HEIGHT_11_08)  <= iHEIGHT((3*NIBBLE_SIZE)-1 downto (2*NIBBLE_SIZE));
            iVID_DATA(HEIGHT_15_12)  <= iHEIGHT((4*NIBBLE_SIZE)-1 downto (3*NIBBLE_SIZE));
            iVID_DATA( WIDTH_03_00)  <= iWIDTH ((1*NIBBLE_SIZE)-1 downto (0*NIBBLE_SIZE));
          end if;

        -- End of the Control Packet
        when CTRL_HEADER_END =>
          if iVID_RDY='1' then
            FSM                       <= DATA_HEADER;
            iVID_SOP                  <= '0';
            iVID_DAV                  <= '1';
            iVID_EOP                  <= '1';
            iVID_DATA                 <= (others => '0');
            -- Height (end) & Type
            iVID_DATA(CTRL_TYPE)      <= x"3";
            iVID_DATA(HEIGHT_03_00)   <= iHEIGHT((1*NIBBLE_SIZE)-1 downto (0*NIBBLE_SIZE));
            iVID_DATA(HEIGHT_07_04)   <= iHEIGHT((2*NIBBLE_SIZE)-1 downto (1*NIBBLE_SIZE));
          end if;

        -- Data Header
        when DATA_HEADER =>
          if iVID_RDY='1' then
            FSM                     <= SEND_BLANK;
            GOTO                    <= SEND_LINE;
            iVID_SOP                <= '1';
            iVID_DAV                <= '1';
            iVID_DATA               <= (others => '0');
          end if;

        -- Blanking phase - wait state between active lines
        when SEND_BLANK =>
          if CNT_BL=BLANK_DUR-2 then
            CNT_BL   <=  0 ;
            RAM_PIX  := 0;
            FSM      <= GOTO;
          else
            CNT_BL   <= CNT_BL + 1;
          end if;
          RAM_ADDR <= std_logic_vector(to_unsigned((RAM_LIN*(LOGO_SIZE_X/2)), RAM_ADDR'length));

        -- Send the active line
        when SEND_LINE =>
          iVID_DAV  <= iVID_RDY;
          iVID_DATA <= (others => '0');
          -- 1st Pixel
          iVID_DATA((01*NB_BITS_SYMB)-1  downto 00*NB_BITS_SYMB+(NB_BITS_SYMB-8)) <= RAM_DATA_B(07 downto 00);
          iVID_DATA((02*NB_BITS_SYMB)-1  downto 01*NB_BITS_SYMB+(NB_BITS_SYMB-8)) <= RAM_DATA_G(07 downto 00);
          iVID_DATA((03*NB_BITS_SYMB)-1  downto 02*NB_BITS_SYMB+(NB_BITS_SYMB-8)) <= RAM_DATA_R(07 downto 00);
          -- 2nd Pixel
          iVID_DATA((04*NB_BITS_SYMB)-1  downto 03*NB_BITS_SYMB+(NB_BITS_SYMB-8)) <= RAM_DATA_B(15 downto 08);
          iVID_DATA((05*NB_BITS_SYMB)-1  downto 04*NB_BITS_SYMB+(NB_BITS_SYMB-8)) <= RAM_DATA_G(15 downto 08);
          iVID_DATA((06*NB_BITS_SYMB)-1  downto 05*NB_BITS_SYMB+(NB_BITS_SYMB-8)) <= RAM_DATA_R(15 downto 08);
          -- Test Pattern Output (combinational)
          if iVID_RDY='1' then
            -- Display icon from RAM
            if RAM_PIX/=((LOGO_SIZE_X/NB_PIX_CLK)-1) then
              RAM_PIX := RAM_PIX + 1;
            end if;
            RAM_ADDR <= std_logic_vector(to_unsigned(((RAM_LIN*(LOGO_SIZE_X/2)) + RAM_PIX), RAM_ADDR'length));
            -- Line / Image progression
            if CNT_Y=(LOGO_SIZE_Y-1) and CNT_X=((LOGO_SIZE_X/2)-1) then -- last pixel of the image
              FSM            <= SEND_BLANK;
              GOTO           <= IDLE;
              iVID_DAV       <= '1';
              iVID_EOP       <= '1';
              CNT_X          <= (others => '0');
            elsif CNT_X=(LOGO_SIZE_X/2) then -- End of line
              FSM            <= SEND_BLANK;
              GOTO           <= SEND_LINE;
              iVID_DAV       <= '0';
              CNT_Y          <= CNT_Y + 1;
              CNT_X          <= (others => '0');
              if RAM_LIN/=(LOGO_SIZE_Y-1) then
                RAM_LIN := RAM_LIN + 1;
              end if;
            else
              CNT_X          <= CNT_X + 1;
            end if;
          end if;

      end case;

    end if;
  end process GEN_proc;

  -- Red Pixel Values
  RAM_LOGO_R_inst : altsyncram
  generic map ( clock_enable_input_a          => "BYPASS"
              , clock_enable_output_a         => "BYPASS"
              , init_file                     => MIF_FILE_NAME_R
              , intended_device_family        => "Arria 10"
              , lpm_hint                      => "ENABLE_RUNTIME_MOD=NO"
              , lpm_type                      => "altsyncram"
              , numwords_a                    => 32768
              , operation_mode                => "SINGLE_PORT"
              , outdata_aclr_a                => "NONE"
              , outdata_reg_a                 => "UNREGISTERED"
              , power_up_uninitialized        => "FALSE"
              , ram_block_type                => "M20K"
              , read_during_write_mode_port_a => "NEW_DATA_NO_NBE_READ"
              , widthad_a                     => 15
              , width_a                       => 16
              , width_byteena_a               => 1
              )
  port map ( address_a => RAM_ADDR
           , clock0    => VID_CLK
           , data_a    => (others => '0')
           , wren_a    => '0'
           , q_a       => RAM_DATA_R
           );

  -- Green Pixel Values
  RAM_LOGO_G_inst : altsyncram
  generic map ( clock_enable_input_a          => "BYPASS"
              , clock_enable_output_a         => "BYPASS"
              , init_file                     => MIF_FILE_NAME_G
              , intended_device_family        => "Arria 10"
              , lpm_hint                      => "ENABLE_RUNTIME_MOD=NO"
              , lpm_type                      => "altsyncram"
              , numwords_a                    => 32768
              , operation_mode                => "SINGLE_PORT"
              , outdata_aclr_a                => "NONE"
              , outdata_reg_a                 => "UNREGISTERED"
              , power_up_uninitialized        => "FALSE"
              , ram_block_type                => "M20K"
              , read_during_write_mode_port_a => "NEW_DATA_NO_NBE_READ"
              , widthad_a                     => 15
              , width_a                       => 16
              , width_byteena_a               => 1
              )
  port map ( address_a => RAM_ADDR
           , clock0    => VID_CLK
           , data_a    => (others => '0')
           , wren_a    => '0'
           , q_a       => RAM_DATA_G
           );

  -- Blue Pixel Values
  RAM_LOGO_B_inst : altsyncram
  generic map ( clock_enable_input_a          => "BYPASS"
              , clock_enable_output_a         => "BYPASS"
              , init_file                     => MIF_FILE_NAME_B
              , intended_device_family        => "Arria 10"
              , lpm_hint                      => "ENABLE_RUNTIME_MOD=NO"
              , lpm_type                      => "altsyncram"
              , numwords_a                    => 32768
              , operation_mode                => "SINGLE_PORT"
              , outdata_aclr_a                => "NONE"
              , outdata_reg_a                 => "UNREGISTERED"
              , power_up_uninitialized        => "FALSE"
              , ram_block_type                => "M20K"
              , read_during_write_mode_port_a => "NEW_DATA_NO_NBE_READ"
              , widthad_a                     => 15
              , width_a                       => 16
              , width_byteena_a               => 1
              )
  port map ( address_a => RAM_ADDR
           , clock0    => VID_CLK
           , data_a    => (others => '0')
           , wren_a    => '0'
           , q_a       => RAM_DATA_B
           );

  -- Video Pixels
  -- ------------
  DATA_proc : process(RAM_DATA_R, RAM_DATA_G, RAM_DATA_B, FSM, iVID_DATA)
  begin
    if FSM=SEND_LINE then
      iVID_DATA2 <= (others => '0');
      -- 1st Pixel
      iVID_DATA2((01*NB_BITS_SYMB)-1  downto 00*NB_BITS_SYMB+(NB_BITS_SYMB-8)) <= RAM_DATA_B(07 downto 00);
      iVID_DATA2((02*NB_BITS_SYMB)-1  downto 01*NB_BITS_SYMB+(NB_BITS_SYMB-8)) <= RAM_DATA_G(07 downto 00);
      iVID_DATA2((03*NB_BITS_SYMB)-1  downto 02*NB_BITS_SYMB+(NB_BITS_SYMB-8)) <= RAM_DATA_R(07 downto 00);
      -- 2nd Pixel
      iVID_DATA2((04*NB_BITS_SYMB)-1  downto 03*NB_BITS_SYMB+(NB_BITS_SYMB-8)) <= RAM_DATA_B(15 downto 08);
      iVID_DATA2((05*NB_BITS_SYMB)-1  downto 04*NB_BITS_SYMB+(NB_BITS_SYMB-8)) <= RAM_DATA_G(15 downto 08);
      iVID_DATA2((06*NB_BITS_SYMB)-1  downto 05*NB_BITS_SYMB+(NB_BITS_SYMB-8)) <= RAM_DATA_R(15 downto 08);
    else
      iVID_DATA2 <= iVID_DATA;
    end if;
  end process DATA_proc;

  -- Combinational output stage
  COMB_OUT_GEN : if not OUTPUT_REG generate

    -- Input ready signal
    iVID_RDY <= VID_RDY;

    -- Streaming interface
    VID_SOP  <= iVID_SOP;
    VID_EOP  <= iVID_EOP;
    VID_DAV  <= iVID_DAV;
    VID_DATA <= iVID_DATA2;

  end generate COMB_OUT_GEN;

  -- Registered output stage
  REG_OUT_GEN : if OUTPUT_REG generate

    constant MAX_WIDTH : natural:=160;
    -- Buffer dimension
    constant BUFFER_DEPTH_LOG2 : natural:=NATURAL(CEIL(LOG2(real(MAX_WIDTH))));
    constant BUFFER_DEPTH      : natural:=2**BUFFER_DEPTH_LOG2;

    -- FIFO Fillrate used to deassert SINK_READY output
    constant ALMOST_FULL : natural:=128;

    component scfifo
    generic (
    add_ram_output_register : string := "OFF";
    allow_rwcycle_when_full : string := "OFF";
    almost_empty_value  : natural := 0;
    almost_full_value : natural := 0;
    intended_device_family  : string := "unused";
    enable_ecc  : string := "FALSE";
    lpm_numwords  : natural;
    lpm_showahead : string := "OFF";
    lpm_width : natural;
    lpm_widthu  : natural := 1;
    overflow_checking : string := "ON";
    ram_block_type  : string := "AUTO";
    underflow_checking  : string := "ON";
    use_eab : string := "ON";
    lpm_hint  : string := "UNUSED";
    lpm_type  : string := "scfifo"
    );
    port(
    aclr  : in std_logic := '0';
    almost_empty  : out std_logic;
    almost_full : out std_logic;
    clock : in std_logic;
    data  : in std_logic_vector(lpm_width-1 downto 0);
    eccstatus : out std_logic_vector(1 downto 0);
    empty : out std_logic;
    full  : out std_logic;
    q : out std_logic_vector(lpm_width-1 downto 0);
    rdreq : in std_logic;
    sclr  : in std_logic := '0';
    usedw : out std_logic_vector(lpm_widthu-1 downto 0);
    wrreq : in std_logic
    );
    end component;

    -- Buffer Signals
    signal WE_EN  : std_logic;
    signal WR_DIN : std_logic_vector(((2*3*NB_BITS_SYMB)+1) downto 0);

    signal FIFO_RD    : std_logic;
    signal FIFO_OUT   : std_logic_vector(((2*3*NB_BITS_SYMB)+1) downto 0);
    signal FIFO_EMPTY : std_logic;
    signal FIFO_NB    : std_logic_vector(BUFFER_DEPTH_LOG2-1 downto 0);

  -----
  Begin
  -----

    -- Video Data Buffer
    ADAPT_BUFFER : scfifo
    generic map ( add_ram_output_register => "ON"
                , allow_rwcycle_when_full => "OFF"
                , intended_device_family  => "Arria 10"
                , enable_ecc              => "FALSE"
                , lpm_numwords            => 256
                , lpm_showahead           => "ON"
                , lpm_width               => 62
                , lpm_widthu              => 8
                , overflow_checking       => "ON"
                , ram_block_type          => "AUTO"
                , underflow_checking      => "ON"
                , use_eab                 => "ON"
                , lpm_hint                => "DISABLE_DCFIFO_EMBEDDED_TIMING_CONSTRAINT=TRUE"
                , lpm_type                => "scfifo"
                )
    port map ( data  => WR_DIN
             , wrreq => WE_EN
             , rdreq => FIFO_RD
             , clock => VID_CLK
             , aclr  => VID_RST
             , q     => FIFO_OUT
             , usedw => FIFO_NB
             , full  => open
             , empty => FIFO_EMPTY
             );

    -- Fifo Read requests
    FIFO_RD  <= (not FIFO_EMPTY) and VID_RDY;

    -- Output stage
    OUT_proc : process(VID_RST, VID_CLK)
    begin
      if VID_RST='1' then
        iVID_RDY <= '1';
        WE_EN    <= '0';
        WR_DIN   <= (others => '0');
        VID_DAV  <= '0';
        VID_EOP  <= '0';
        VID_SOP  <= '0';
        VID_DATA <= (others => '0');
      elsif rising_edge(VID_CLK) then

        -- Ready to RAM reader
        iVID_RDY <= '1';
        if (unsigned(FIFO_NB) > ALMOST_FULL) then
          iVID_RDY <= '0';
        end if;

        -- Data to store
        WE_EN  <= iVID_DAV;
        WR_DIN <= iVID_SOP & iVID_DATA2 & iVID_EOP;

        -- Output signals
        VID_SOP  <= '0';
        VID_DAV  <= '0';
        VID_EOP  <= '0';
        if FIFO_RD='1' then
          VID_DAV  <= '1';
          VID_SOP  <= FIFO_OUT(FIFO_OUT'high);
          VID_EOP  <= FIFO_OUT(FIFO_OUT'low);
          VID_DATA <= FIFO_OUT(FIFO_OUT'high-1 downto FIFO_OUT'low+1);
        end if;

      end if;
    end process OUT_proc;

  end generate REG_OUT_GEN;

-- ---------------------
   End architecture RTL;
-- ---------------------
