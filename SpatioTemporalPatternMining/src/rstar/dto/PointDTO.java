package rstar.dto;

public class PointDTO extends AbstractDTO{
    public float oid;
    public float[] coords;
    public String label;

    public PointDTO(float oid, float[] coords, String label) {
        this.oid = oid;
        this.coords = coords;
        this.label = label;
    }
}
