package net.shenjin.objection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.shenjin.objection.init.ObjectionModParticleTypes;
import net.shenjin.objection.init.ObjectionModSounds;

import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

import java.util.HashMap;
import java.util.Map;

@Mod("objection")
public class ObjectionMod {
	public static final Logger LOGGER = LogManager.getLogger(ObjectionMod.class);
	public static final String MODID = "objection";

	public ObjectionMod(IEventBus modEventBus) {
		// Start of user code block mod constructor
		// End of user code block mod constructor
		modEventBus.addListener(this::registerNetworking);
		ObjectionModSounds.REGISTRY.register(modEventBus);

		ObjectionModParticleTypes.REGISTRY.register(modEventBus);

		LOGGER.info("[Objection] initialised: sounds/particles registered");

		// Start of user code block mod init
		// End of user code block mod init
	}

	// Start of user code block mod methods
	// End of user code block mod methods
	private static boolean networkingRegistered = false;
	private static final Map<CustomPacketPayload.Type<?>, NetworkMessage<?>> MESSAGES = new HashMap<>();

	private record NetworkMessage<T extends CustomPacketPayload>(StreamCodec<? extends FriendlyByteBuf, T> reader, IPayloadHandler<T> handler) {
	}

	public static <T extends CustomPacketPayload> void addNetworkMessage(CustomPacketPayload.Type<T> id, StreamCodec<? extends FriendlyByteBuf, T> reader, IPayloadHandler<T> handler) {
		if (networkingRegistered)
			throw new IllegalStateException("Cannot register new network messages after networking has been registered");
		MESSAGES.put(id, new NetworkMessage<>(reader, handler));
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private void registerNetworking(final RegisterPayloadHandlersEvent event) {
		final PayloadRegistrar registrar = event.registrar(MODID);
		MESSAGES.forEach((id, networkMessage) -> registrar.playBidirectional(id, ((NetworkMessage) networkMessage).reader(), ((NetworkMessage) networkMessage).handler()));
		networkingRegistered = true;
	}
}
