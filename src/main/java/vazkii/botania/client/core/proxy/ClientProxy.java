/*
 * This class is distributed as part of the Botania Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 */
package vazkii.botania.client.core.proxy;

import net.minecraft.block.Block;
import net.minecraft.block.FlowerBlock;
import net.minecraft.block.TallFlowerBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeBuffers;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.client.renderer.entity.PlayerRenderer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.particles.IParticleData;
import net.minecraft.util.IItemProvider;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DeferredWorkQueue;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import org.lwjgl.glfw.GLFW;

import vazkii.botania.client.core.handler.*;
import vazkii.botania.client.core.helper.RenderHelper;
import vazkii.botania.client.core.helper.ShaderHelper;
import vazkii.botania.client.fx.FXLightning;
import vazkii.botania.client.fx.ModParticles;
import vazkii.botania.client.render.world.SkyblockRenderEvents;
import vazkii.botania.common.Botania;
import vazkii.botania.common.block.ModBlocks;
import vazkii.botania.common.block.ModFluffBlocks;
import vazkii.botania.common.block.decor.BlockFloatingFlower;
import vazkii.botania.common.block.decor.BlockModMushroom;
import vazkii.botania.common.block.mana.BlockPool;
import vazkii.botania.common.block.subtile.functional.BergamuteEventHandler;
import vazkii.botania.common.core.handler.ConfigHandler;
import vazkii.botania.common.core.helper.ItemNBTHelper;
import vazkii.botania.common.core.helper.Vector3;
import vazkii.botania.common.core.proxy.IProxy;
import vazkii.botania.common.entity.EntityDoppleganger;
import vazkii.botania.common.item.*;
import vazkii.botania.common.item.brew.ItemBrewBase;
import vazkii.botania.common.item.equipment.bauble.ItemDodgeRing;
import vazkii.botania.common.item.equipment.bauble.ItemMagnetRing;
import vazkii.botania.common.item.equipment.bauble.ItemMonocle;
import vazkii.botania.common.item.equipment.tool.bow.ItemLivingwoodBow;
import vazkii.botania.common.item.equipment.tool.terrasteel.ItemTerraAxe;
import vazkii.botania.common.item.equipment.tool.terrasteel.ItemTerraPick;
import vazkii.botania.common.item.relic.ItemInfiniteFruit;
import vazkii.botania.common.item.rod.ItemTornadoRod;
import vazkii.botania.common.lib.LibMisc;
import vazkii.botania.common.lib.LibObfuscation;
import vazkii.patchouli.api.IMultiblock;
import vazkii.patchouli.api.PatchouliAPI;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;

import static vazkii.botania.common.lib.ResourceLocationHelper.prefix;

public class ClientProxy implements IProxy {

	public static boolean jingleTheBells = false;
	public static boolean dootDoot = false;
	private static final MethodHandle REGISTER_PROPERTY_GETTER = LibObfuscation.getMethod(ItemModelsProperties.class, "func_239418_a_", Item.class, ResourceLocation.class, IItemPropertyGetter.class);

	public static KeyBinding CORPOREA_REQUEST;

	@Override
	public void registerHandlers() {
		// This is the only place it works, but mods are constructed in parallel (brilliant idea) so this
		// *could* end up blowing up if it races with someone else. Let's pray that doesn't happen.
		ShaderHelper.initShaders();

		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		modBus.addListener(this::clientSetup);
		modBus.addListener(this::loadComplete);
		modBus.addListener(MiscellaneousIcons.INSTANCE::onTextureStitchPre);
		modBus.addListener(MiscellaneousIcons.INSTANCE::onTextureStitchPost);
		modBus.addListener(MiscellaneousIcons.INSTANCE::onModelRegister);
		modBus.addListener(MiscellaneousIcons.INSTANCE::onModelBake);
		modBus.addListener(SplashHandler::registerFactories);
		modBus.addListener(ModelHandler::registerModels);
		modBus.addListener(ModParticles.FactoryHandler::registerFactories);

		IEventBus forgeBus = MinecraftForge.EVENT_BUS;
		forgeBus.addListener(EventPriority.HIGHEST, TooltipHandler::onTooltipEvent);
		forgeBus.addListener(TooltipAdditionDisplayHandler::onToolTipRender);
		forgeBus.addListener(RenderLexicon::renderHand);
		forgeBus.addListener(LightningHandler::onRenderWorldLast);
		forgeBus.addListener(KonamiHandler::clientTick);
		forgeBus.addListener(KonamiHandler::handleInput);
		forgeBus.addListener(KonamiHandler::renderBook);
		forgeBus.addListener(HUDHandler::onDrawScreenPost);
		forgeBus.addListener(DebugHandler::onDrawDebugText);
		forgeBus.addListener(CorporeaInputHandler::buttonPressed);
		forgeBus.addListener(ClientTickHandler::clientTickEnd);
		forgeBus.addListener(ClientTickHandler::renderTick);
		forgeBus.addListener(BoundTileRenderer::onWorldRenderLast);
		forgeBus.addListener(BossBarHandler::onBarRender);
		forgeBus.addListener(BlockHighlightRenderHandler::onWorldRenderLast);
		forgeBus.addListener(AstrolabePreviewHandler::onWorldRenderLast);
		forgeBus.addListener(SkyblockRenderEvents::onRender);
		forgeBus.addListener(ItemDodgeRing::onKeyDown);
		forgeBus.addListener(EventPriority.LOWEST, BergamuteEventHandler::onSoundEvent);
	}

	private void clientSetup(FMLClientSetupEvent event) {
		PersistentVariableHelper.setCacheFile(new File(Minecraft.getInstance().gameDir, "BotaniaVars.dat"));
		try {
			PersistentVariableHelper.load();
			PersistentVariableHelper.save();
		} catch (IOException e) {
			Botania.LOGGER.fatal("Persistent Variables couldn't load!!");
		}

		if (ConfigHandler.CLIENT.enableSeasonalFeatures.get()) {
			LocalDateTime now = LocalDateTime.now();
			if (now.getMonth() == Month.DECEMBER && now.getDayOfMonth() >= 16 || now.getMonth() == Month.JANUARY && now.getDayOfMonth() <= 2) {
				jingleTheBells = true;
			}
			if (now.getMonth() == Month.OCTOBER) {
				dootDoot = true;
			}
		}

		registerRenderTypes();

		DeferredWorkQueue.runLater(() -> {
			CORPOREA_REQUEST = new KeyBinding("key.botania_corporea_request", KeyConflictContext.GUI, InputMappings.getInputByCode(GLFW.GLFW_KEY_C, 0), LibMisc.MOD_NAME);
			ClientRegistry.registerKeyBinding(ClientProxy.CORPOREA_REQUEST);
			registerPropertyGetters();
		});

	}

	private static void registerPropertyGetter(IItemProvider item, ResourceLocation id, IItemPropertyGetter propGetter) {
		try {
			REGISTER_PROPERTY_GETTER.invokeExact(item.asItem(), id, propGetter);
		} catch (Throwable throwable) {
			throw new RuntimeException(throwable);
		}
	}

	private static void registerPropertyGetters() {
		registerPropertyGetter(ModItems.blackHoleTalisman, prefix("active"),
						(stack, world, entity) -> ItemNBTHelper.getBoolean(stack, ItemBlackHoleTalisman.TAG_ACTIVE, false) ? 1 : 0);
		registerPropertyGetter(ModItems.manaBottle, prefix("swigs_taken"),
						(stack, world, entity) -> ItemBottledMana.SWIGS - ItemBottledMana.getSwigsLeft(stack));

		ResourceLocation vuvuzelaId = prefix("vuvuzela");
		IItemPropertyGetter isVuvuzela = (stack, world, entity) -> stack.getDisplayName().getString().toLowerCase(Locale.ROOT).contains("vuvuzela") ? 1 : 0;
		registerPropertyGetter(ModItems.grassHorn, vuvuzelaId, isVuvuzela);
		registerPropertyGetter(ModItems.leavesHorn, vuvuzelaId, isVuvuzela);
		registerPropertyGetter(ModItems.snowHorn, vuvuzelaId, isVuvuzela);

		registerPropertyGetter(ModItems.lexicon, prefix("elven"), (stack, world, living) -> ModItems.lexicon.isElvenItem(stack) ? 1 : 0);
		registerPropertyGetter(ModItems.manaCookie, prefix("totalbiscuit"),
						(stack, world, entity) -> stack.getDisplayName().getString().toLowerCase(Locale.ROOT).contains("totalbiscuit") ? 1F : 0F);
		registerPropertyGetter(ModItems.slimeBottle, prefix("active"),
						(stack, world, entity) -> stack.hasTag() && stack.getTag().getBoolean(ItemSlimeBottle.TAG_ACTIVE) ? 1.0F : 0.0F);
		registerPropertyGetter(ModItems.spawnerMover, prefix("full"),
						(stack, world, entity) -> ItemSpawnerMover.hasData(stack) ? 1 : 0);
		registerPropertyGetter(ModItems.temperanceStone, prefix("active"),
						(stack, world, entity) -> ItemNBTHelper.getBoolean(stack, ItemTemperanceStone.TAG_ACTIVE, false) ? 1 : 0);
		registerPropertyGetter(ModItems.twigWand, prefix("bindmode"),
						(stack, world, entity) -> ItemTwigWand.getBindMode(stack) ? 1 : 0);

		ResourceLocation poolFullId = prefix("full");
		IItemPropertyGetter poolFull = (stack, world, entity) -> {
			Block block = ((BlockItem) stack.getItem()).getBlock();
			boolean renderFull = ((BlockPool) block).variant == BlockPool.Variant.CREATIVE || stack.hasTag() && stack.getTag().getBoolean("RenderFull");
			return renderFull ? 1F : 0F;
		};
		registerPropertyGetter(ModBlocks.manaPool, poolFullId, poolFull);
		registerPropertyGetter(ModBlocks.dilutedPool, poolFullId, poolFull);
		registerPropertyGetter(ModBlocks.creativePool, poolFullId, poolFull);
		registerPropertyGetter(ModBlocks.fabulousPool, poolFullId, poolFull);

		IItemPropertyGetter brewGetter = (stack, world, entity) -> {
			ItemBrewBase item = ((ItemBrewBase) stack.getItem());
			return item.getSwigs() - item.getSwigsLeft(stack);
		};
		registerPropertyGetter(ModItems.brewVial, prefix("swigs_taken"), brewGetter);
		registerPropertyGetter(ModItems.brewFlask, prefix("swigs_taken"), brewGetter);

		ResourceLocation holidayId = prefix("holiday");
		IItemPropertyGetter holidayGetter = (stack, worldIn, entityIn) -> ClientProxy.jingleTheBells ? 1 : 0;
		registerPropertyGetter(ModItems.manaweaveHelm, holidayId, holidayGetter);
		registerPropertyGetter(ModItems.manaweaveChest, holidayId, holidayGetter);
		registerPropertyGetter(ModItems.manaweaveBoots, holidayId, holidayGetter);
		registerPropertyGetter(ModItems.manaweaveLegs, holidayId, holidayGetter);

		IItemPropertyGetter ringOnGetter = (stack, worldIn, entityIn) -> ItemMagnetRing.getCooldown(stack) <= 0 ? 1 : 0;
		registerPropertyGetter(ModItems.magnetRing, prefix("on"), ringOnGetter);
		registerPropertyGetter(ModItems.magnetRingGreater, prefix("on"), ringOnGetter);

		registerPropertyGetter(ModItems.elementiumShears, prefix("reddit"),
						(stack, world, entity) -> stack.getDisplayName().getString().equalsIgnoreCase("dammit reddit") ? 1F : 0F);
		registerPropertyGetter(ModItems.manasteelSword, prefix("elucidator"),
						(stack, world, entity) -> "the elucidator".equals(stack.getDisplayName().getString().toLowerCase().trim()) ? 1 : 0);
		registerPropertyGetter(ModItems.terraAxe, prefix("terraaxe_on"),
						(stack, world, entity) -> entity instanceof PlayerEntity && !ItemTerraAxe.shouldBreak((PlayerEntity) entity) ? 0 : 1);
		registerPropertyGetter(ModItems.terraPick, prefix("tipped"),
						(stack, world, entity) -> ItemTerraPick.isTipped(stack) ? 1 : 0);
		registerPropertyGetter(ModItems.terraPick, prefix("enabled"),
						(stack, world, entity) -> ItemTerraPick.isEnabled(stack) ? 1 : 0);
		registerPropertyGetter(ModItems.infiniteFruit, prefix("boot"),
						(stack, worldIn, entity) -> ItemInfiniteFruit.isBoot(stack) ? 1F : 0F);
		registerPropertyGetter(ModItems.tornadoRod, prefix("flying"),
						(stack, world, living) -> ItemTornadoRod.isFlying(stack) ? 1 : 0);

		IItemPropertyGetter pulling = ItemModelsProperties.func_239417_a_(Items.BOW, new ResourceLocation("pulling"));
		IItemPropertyGetter pull = (stack, worldIn, entity) -> {
			if (entity == null) {
				return 0.0F;
			} else {
				ItemLivingwoodBow item = ((ItemLivingwoodBow) stack.getItem());
				return entity.getActiveItemStack() != stack
								? 0.0F
								: (stack.getUseDuration() - entity.getItemInUseCount()) * item.chargeVelocityMultiplier() / 20.0F;
			}
		};
		registerPropertyGetter(ModItems.livingwoodBow, new ResourceLocation("pulling"), pulling);
		registerPropertyGetter(ModItems.livingwoodBow, new ResourceLocation("pull"), pull);
		registerPropertyGetter(ModItems.crystalBow, new ResourceLocation("pulling"), pulling);
		registerPropertyGetter(ModItems.crystalBow, new ResourceLocation("pull"), pull);
	}

	private static void registerRenderTypes() {
		RenderTypeLookup.setRenderLayer(ModBlocks.defaultAltar, RenderType.getCutout());
		RenderTypeLookup.setRenderLayer(ModBlocks.forestAltar, RenderType.getCutout());
		RenderTypeLookup.setRenderLayer(ModBlocks.plainsAltar, RenderType.getCutout());
		RenderTypeLookup.setRenderLayer(ModBlocks.mountainAltar, RenderType.getCutout());
		RenderTypeLookup.setRenderLayer(ModBlocks.fungalAltar, RenderType.getCutout());
		RenderTypeLookup.setRenderLayer(ModBlocks.swampAltar, RenderType.getCutout());
		RenderTypeLookup.setRenderLayer(ModBlocks.desertAltar, RenderType.getCutout());
		RenderTypeLookup.setRenderLayer(ModBlocks.taigaAltar, RenderType.getCutout());
		RenderTypeLookup.setRenderLayer(ModBlocks.mesaAltar, RenderType.getCutout());
		RenderTypeLookup.setRenderLayer(ModBlocks.mossyAltar, RenderType.getCutout());
		RenderTypeLookup.setRenderLayer(ModBlocks.ghostRail, RenderType.getCutout());
		RenderTypeLookup.setRenderLayer(ModBlocks.solidVines, RenderType.getCutout());

		RenderTypeLookup.setRenderLayer(ModBlocks.manaGlass, RenderType.getTranslucent());
		RenderTypeLookup.setRenderLayer(ModFluffBlocks.managlassPane, RenderType.getTranslucent());
		RenderTypeLookup.setRenderLayer(ModBlocks.elfGlass, RenderType.getTranslucent());
		RenderTypeLookup.setRenderLayer(ModFluffBlocks.alfglassPane, RenderType.getTranslucent());
		RenderTypeLookup.setRenderLayer(ModBlocks.bifrost, RenderType.getTranslucent());
		RenderTypeLookup.setRenderLayer(ModFluffBlocks.bifrostPane, RenderType.getTranslucent());
		RenderTypeLookup.setRenderLayer(ModBlocks.bifrostPerm, RenderType.getTranslucent());
		RenderTypeLookup.setRenderLayer(ModBlocks.prism, RenderType.getTranslucent());

		RenderTypeLookup.setRenderLayer(ModBlocks.starfield, RenderType.getCutoutMipped());
		RenderTypeLookup.setRenderLayer(ModBlocks.abstrusePlatform, t -> true);
		RenderTypeLookup.setRenderLayer(ModBlocks.infrangiblePlatform, t -> true);
		RenderTypeLookup.setRenderLayer(ModBlocks.spectralPlatform, t -> true);

		Registry.BLOCK.stream().filter(b -> Registry.BLOCK.getKey(b).getNamespace().equals(LibMisc.MOD_ID))
				.forEach(b -> {
					if (b instanceof BlockFloatingFlower || b instanceof FlowerBlock
							|| b instanceof TallFlowerBlock || b instanceof BlockModMushroom) {
						RenderTypeLookup.setRenderLayer(b, RenderType.getCutout());
					}
				});
	}

	private void loadComplete(FMLLoadCompleteEvent event) {
		DeferredWorkQueue.runLater(() -> {
			initAuxiliaryRender();
			ColorHandler.init();

			// Needed to prevent mana pools on carts from X-raying through the cart
			SortedMap<RenderType, BufferBuilder> layers = ObfuscationReflectionHelper.getPrivateValue(RenderTypeBuffers.class, Minecraft.getInstance().getRenderTypeBuffers(), "field_228480_b_");
			layers.put(RenderHelper.MANA_POOL_WATER, new BufferBuilder(RenderHelper.MANA_POOL_WATER.getBufferSize()));
		});
	}

	private void initAuxiliaryRender() {
		Map<String, PlayerRenderer> skinMap = Minecraft.getInstance().getRenderManager().getSkinMap();
		PlayerRenderer render;
		render = skinMap.get("default");
		render.addLayer(new ContributorFancinessHandler(render));
		if (Botania.curiosLoaded) {
			render.addLayer(new BaubleRenderHandler(render));
		}
		render.addLayer(new LayerTerraHelmet(render));

		render = skinMap.get("slim");
		render.addLayer(new ContributorFancinessHandler(render));
		if (Botania.curiosLoaded) {
			render.addLayer(new BaubleRenderHandler(render));
		}
		render.addLayer(new LayerTerraHelmet(render));
	}

	@Override
	public boolean isTheClientPlayer(LivingEntity entity) {
		return entity == Minecraft.getInstance().player;
	}

	@Override
	public PlayerEntity getClientPlayer() {
		return Minecraft.getInstance().player;
	}

	@Override
	public boolean isClientPlayerWearingMonocle() {
		return ItemMonocle.hasMonocle(Minecraft.getInstance().player);
	}

	@Override
	public long getWorldElapsedTicks() {
		return ClientTickHandler.ticksInGame;
	}

	@Override
	public void lightningFX(Vector3 vectorStart, Vector3 vectorEnd, float ticksPerMeter, long seed, int colorOuter, int colorInner) {
		Minecraft.getInstance().particles.addEffect(new FXLightning(Minecraft.getInstance().world, vectorStart, vectorEnd, ticksPerMeter, seed, colorOuter, colorInner));
	}

	@Override
	public void addBoss(EntityDoppleganger boss) {
		BossBarHandler.bosses.add(boss);
	}

	@Override
	public void removeBoss(EntityDoppleganger boss) {
		BossBarHandler.bosses.remove(boss);
	}

	@Override
	public int getClientRenderDistance() {
		return Minecraft.getInstance().gameSettings.renderDistanceChunks;
	}

	@Override
	public void addParticleForce(World world, IParticleData particleData, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
		world.addParticle(particleData, true, x, y, z, xSpeed, ySpeed, zSpeed);
	}

	@Override
	public void showMultiblock(IMultiblock mb, ITextComponent name, BlockPos anchor, Rotation rot) {
		PatchouliAPI.instance.showMultiblock(mb, name, anchor, rot);
	}

	@Override
	public void clearSextantMultiblock() {
		IMultiblock mb = PatchouliAPI.instance.getCurrentMultiblock();
		if (mb != null && mb.getID().equals(ItemSextant.MULTIBLOCK_ID)) {
			PatchouliAPI.instance.clearMultiblock();
		}
	}
}
