package org.example.dto.save;

import java.util.ArrayList;
import java.util.List;

public class GroupDTO extends LayerDTO {
    private List<LayerDTO> children = new ArrayList<>();

    public GroupDTO() {
    }

    public List<LayerDTO> getChildren() {
        return children;
    }

    public void setChildren(List<LayerDTO> children) {
        this.children = children;
    }

    public void addChild(LayerDTO child) {
        this.children.add(child);
    }

    @Override
    public LayerDTO deepCopy() {
        GroupDTO copy = new GroupDTO();
        copy.setX(getX());
        copy.setY(getY());
        copy.setScaleX(getScaleX());
        copy.setScaleY(getScaleY());
        copy.setShearX(getShearX());
        copy.setShearY(getShearY());
        copy.setCustomPivotX(getCustomPivotX());
        copy.setCustomPivotY(getCustomPivotY());
        copy.setRotation(getRotation());
        copy.setZIndex(getZIndex());
        copy.setLocked(isLocked());
        copy.setActiveZone(getActiveZone());
        copy.setWidth(getWidth());
        copy.setHeight(getHeight());

        if (this.children != null) {
            for (LayerDTO child : this.children) {
                copy.addChild(child.deepCopy());
            }
        }
        return copy;
    }
}

