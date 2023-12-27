package phanastrae.operation_starcleave.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.Rarity;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.joml.Math;
import phanastrae.operation_starcleave.world.OperationStarcleaveWorld;
import phanastrae.operation_starcleave.world.firmament.*;

import static phanastrae.operation_starcleave.world.firmament.FirmamentSubRegion.TILE_SIZE;

public class FirmamentManipulatorItem extends Item {

    public FirmamentManipulatorItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);

        if(world.isClient) {
            Firmament firmament = Firmament.fromWorld(world);

            if (user.isSneaking()) {
                firmament.init();
            } else {
                float pitch = Math.toRadians(user.getPitch());
                if(pitch > 0) return TypedActionResult.fail(itemStack);
                float yaw = Math.toRadians(user.getYaw());

                float sinYaw = Math.sin(yaw);
                float cosYaw = Math.cos(yaw);
                float sinPitch = Math.sin(pitch);
                float cosPitch = Math.cos(pitch);

                Vec3d lookVec = new Vec3d(-sinYaw * cosPitch, -sinPitch, cosYaw * cosPitch);

                Vec3d pos = user.getPos();

                float skyHeight = world.getTopY() + 16;
                double t = (skyHeight - pos.y) / lookVec.y;
                Vec3d target = pos.add(lookVec.multiply(t));

                Random random = user.getRandom();
                for(int i = 0; i < 10; i++) {
                    world.addParticle(ParticleTypes.EXPLOSION, target.x+random.nextFloat() * 4 - 2, target.y+random.nextFloat() * 1 - 0.5f, target.z+random.nextFloat() * 4 - 2, 0, 0, 0);
                }

                int x = (int)target.x;
                int z = (int)target.z;
                formCrack(firmament, x, z, random);
            }
        }

        return TypedActionResult.success(itemStack);
    }

    public static void formCrack(Firmament firmament, int x, int z, Random random) {
        ((OperationStarcleaveWorld)firmament.getWorld()).operation_starcleave$setCleavingFlashTicksLeft(24);

        firmament.setDamage(x, z, 1);
        int rad = 15;
        for(int i = -rad; i <= rad; i++) {
            for(int j = -rad; j <= rad; j++) {
                float distSqr = i*i + j*j;

                if(distSqr > rad*rad) continue;
                float fallOff = 1 - (distSqr)/(rad*rad);
                firmament.setDrip(x+i*TILE_SIZE, z+j*TILE_SIZE, firmament.getDrip(x+i*TILE_SIZE, z+j*TILE_SIZE) + (int)(0.01f * fallOff * fallOff * fallOff * 16f) / 16f);
            }
        }

        float phase = random.nextFloat();
        int count = 10;
        for(int i = 0; i < count; i++) {
            float theta = (phase + i / (float)count) * 2 * (float)Math.PI;
            FirmamentActor actor = new FirmamentActor(firmament, x, z, Math.cos(theta)*TILE_SIZE, Math.sin(theta)*TILE_SIZE, 40);
            actor.initialDelay = 12;
            firmament.addActor(actor);
        }
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }
}
