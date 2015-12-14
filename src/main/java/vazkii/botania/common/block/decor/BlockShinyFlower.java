/**
 * This class was created by <Vazkii>. It's distributed as
 * part of the Botania Mod. Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 * 
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 * 
 * File Created @ [Jun 2, 2014, 8:15:49 PM (GMT)]
 */
package vazkii.botania.common.block.decor;

import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import thaumcraft.api.crafting.IInfusionStabiliser;
import vazkii.botania.api.item.IHornHarvestable;
import vazkii.botania.api.lexicon.LexiconEntry;
import vazkii.botania.api.state.BotaniaStateProps;
import vazkii.botania.api.state.enums.PrismarineVariant;
import vazkii.botania.client.core.helper.IconHelper;
import vazkii.botania.common.block.BlockModFlower;
import vazkii.botania.common.core.handler.ConfigHandler;
import vazkii.botania.common.integration.coloredlights.ColoredLightHelper;
import vazkii.botania.common.lexicon.LexiconData;
import vazkii.botania.common.lib.LibBlockNames;
import net.minecraftforge.fml.common.Optional;

@Optional.Interface(modid = "Thaumcraft", iface = "thaumcraft.api.crafting.IInfusionStabiliser", striprefs = true)
public class BlockShinyFlower extends BlockModFlower implements IInfusionStabiliser, IHornHarvestable {

	private static IIcon[] icons;
	private static IIcon[] iconsAlt;

	public BlockShinyFlower() {
		super(LibBlockNames.SHINY_FLOWER);
		setLightLevel(1F);
		setDefaultState(blockState.getBaseState().withProperty(BotaniaStateProps.COLOR, EnumDyeColor.WHITE));
	}

	@Override
	public BlockState createBlockState() {
		return new BlockState(this, BotaniaStateProps.COLOR);
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return ((EnumDyeColor) state.getValue(BotaniaStateProps.COLOR)).getMetadata();
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {
		if (meta >= EnumDyeColor.values().length) {
			meta = 0;
		}
		return getDefaultState().withProperty(BotaniaStateProps.COLOR, EnumDyeColor.byMetadata(meta));
	}

	@Override
	@Optional.Method(modid = "easycoloredlights")
	public int getLightValue(IBlockAccess world, BlockPos pos) {
		return ColoredLightHelper.getPackedColor(((EnumDyeColor) world.getBlockState(pos).getValue(BotaniaStateProps.COLOR)), originalLight);
	}

	@Override
	public LexiconEntry getEntry(World world, BlockPos pos, EntityPlayer player, ItemStack lexicon) {
		return LexiconData.shinyFlowers;
	}

	@Override
	public boolean canGrow(World world, BlockPos pos, IBlockState state, boolean fuckifiknow) {
		return false;
	}

	@Override
	public boolean canStabaliseInfusion(World world, BlockPos pos) {
		return true;
	}

	@Override
	public boolean canHornHarvest(World world, BlockPos pos, ItemStack stack, EnumHornType hornType) {
		return false;
	}

	@Override
	public boolean hasSpecialHornHarvest(World world, BlockPos pos, ItemStack stack, EnumHornType hornType) {
		return false;
	}

	@Override
	public void harvestByHorn(World world, BlockPos pos, ItemStack stack, EnumHornType hornType) {
		// NO-OP
	}

}
