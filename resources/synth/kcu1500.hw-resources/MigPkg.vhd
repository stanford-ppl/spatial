-------------------------------------------------------------------------------
-- File       : MigPkg.vhd
-- Company    : SLAC National Accelerator Laboratory
-------------------------------------------------------------------------------
-- Description: Package file for MIG Core
-------------------------------------------------------------------------------
-- This file is part of 'axi-pcie-core'.
-- It is subject to the license terms in the LICENSE.txt file found in the 
-- top-level directory of this distribution and at: 
--    https://confluence.slac.stanford.edu/display/ppareg/LICENSE.html. 
-- No part of 'axi-pcie-core', including this file, 
-- may be copied, modified, propagated, or distributed except according to 
-- the terms contained in the LICENSE.txt file.
-------------------------------------------------------------------------------

library ieee;
use ieee.std_logic_1164.all;

use work.StdRtlPkg.all;
use work.AxiLitePkg.all;
use work.AxiStreamPkg.all;
use work.AxiPkg.all;

package MigPkg is

   -- DDR MEM AXI Configuration
   constant MEM_AXI_CONFIG_C : AxiConfigType := (
      ADDR_WIDTH_C => 32,               -- 4GB per MIG interface (16GB total)
      DATA_BYTES_C => 64,               -- 512-bit data interface
      ID_BITS_C    => 4,                -- Up to 16 IDS
      LEN_BITS_C   => 8);               -- 8-bit awlen/arlen interface  

   -- DDR Port Types
   type DdrOutType is record
      addr : slv(16 downto 0);
      ba   : slv(1 downto 0);
      cke  : slv(0 downto 0);
      csL  : slv(1 downto 0);
      odt  : slv(0 downto 0);
      bg   : slv(0 downto 0);
      rstL : sl;
      actL : sl;
      ckC  : slv(0 downto 0);
      ckT  : slv(0 downto 0);
   end record DdrOutType;
   type DdrOutArray is array (natural range<>) of DdrOutType;
   constant DDR_OUT_INIT_C : DdrOutType := (
      addr => (others => '1'),
      ba   => (others => '1'),
      cke  => (others => '1'),
      csL  => (others => '1'),
      odt  => (others => '1'),
      bg   => (others => '1'),
      rstL => '1',
      actL => '1',
      ckC  => (others => '1'),
      ckT  => (others => '1'));

   type DdrInOutType is record
      dm   : slv(8 downto 0);
      dq   : slv(71 downto 0);
      dqsC : slv(8 downto 0);
      dqsT : slv(8 downto 0);
   end record DdrInOutType;
   type DdrInOutArray is array (natural range<>) of DdrInOutType;

end package MigPkg;
