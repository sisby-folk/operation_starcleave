package phanastrae.operation_starcleave.world.firmament;

import java.util.Random;

public class Actor {

    public Actor(int originX, int originZ, float dx, float dz, float damagePotential) {
        this.originX = originX;
        this.originZ = originZ;
        this.dx = dx;
        this.dz = dz;
        this.damagePotential = damagePotential;
    }


    public int originX;
    public int originZ;
    public float dx;
    public float dz;

    public float damagePotential;

    public Random random = new Random();

    public boolean active = true;

    public void tick(FirmamentRegion fr) {
        if (this.damagePotential < 0.0001f) {
            this.discard();
        }
        if(!this.active) return;

        if(random.nextFloat() > 0.9f) {
            split(fr);
            discard();
            return;
        }

        double theta = random.nextFloat() * Math.PI * 2;

        float ddx = (float)Math.cos(theta);
        float ddz = (float)Math.sin(theta);
        float dSqr = dx * dx + dz * dz;
        float dNorm = dSqr == 0 ? 0 : 1/(float)Math.sqrt(dSqr);
        float dot = (dx * ddx + dz * ddz) * dNorm;

        float f = dot * dot * dot;

        int n = random.nextInt(4, 12);
        for(int i = 0; i < n; i++) {
            float dFdx = fr.dFdxBigDamage(originX+(int)dx, originZ+(int)dz);
            float dFdz = fr.dFdzBigDamage(originX+(int)dx, originZ+(int)dz);
            float dFSqr = dFdx * dFdx + dFdz * dFdz;
            float dFnorm = dFSqr == 0 ? 0 : 1/(float)Math.sqrt(dFSqr);
            float dot2 = (ddx * dFdx + ddz * dFdz) * dFnorm;

            float g = (1 - dot2 * dot2 * dot2) / 2;

            dx += ddx * f * g;
            dz += ddz * f * g;

            int idx = originX + (int) dx;
            int idz = originZ + (int) dz;

            float damage = fr.getDamage(idx, idz);
            float addDamage = Math.min(damagePotential, 1 - damage);
            addDamage *= 0.2f;
            float newDamage = fr.getDamage(idx, idz) + addDamage;
            if(newDamage > 1) newDamage = 1;
            fr.setDamage(idx, idz, newDamage);
            fr.markActive(idx, idz);

            float finalAddDamage = addDamage;
            fr.forEachNeighbor((nx, nz, nWeight) -> {
                fr.setDrip(idx+nx, idz+nz, fr.getDrip(idx+nx, idz+nz) + (int)(finalAddDamage * nWeight * 16f) / 16f);
                fr.markActive(idx+nx, idz+nz);
            });
            this.damagePotential -= addDamage;
        }
    }

    public void discard() {
        this.active = false;
    }

    public void split(FirmamentRegion fr) {
        int hdx = (int)(dx * 0.9f);
        int hdz = (int)(dz * 0.9f);

        int ddp = (int)(damagePotential * (random.nextFloat() * 0.6f + 0.3f));

        float dx2 = dx - hdx;
        float dz2 = dz - hdz;

        Actor actor1 = new Actor(originX+hdx, originZ+hdz, dx2, dz2, damagePotential - ddp);
        Actor actor2 = new Actor(originX+hdx, originZ+hdz, dx2, dz2, ddp);

        fr.addActor(actor1);
        fr.addActor(actor2);
    }
}
