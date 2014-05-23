package rstar.dto;

public class PointDTO extends AbstractDTO{
    public float oid;
    public float[] coords;
    public String label;
    public long time;

    public PointDTO(float oid, float[] coords, String label, long time) {
        this.oid = oid;
        this.coords = coords;
        this.label = label;
        this.time = time;
    }
}
