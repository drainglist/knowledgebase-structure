package com.polytech.knowledgebase.structure.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Check;
import java.util.*;

@Entity
@Table(name = "sources")
@Check(constraints = "length(trim(id)) > 0 and length(trim(title)) > 0 and length(trim(location)) > 0")
public class Source extends BaseEntity {
    @Column(nullable = false, length = 300) private String title;
    @Column(nullable = false, length = 1000, unique = true) private String location;
    @Column(nullable = false) private boolean demonstration;
    protected Source() {}
    public Source(String id, String title, String location, boolean demonstration) {
        super(id); this.title = required(title, "title", 300); this.location = required(location, "location", 1000); this.demonstration = demonstration;
    }
    public String getTitle() { return title; } public String getLocation() { return location; }
    public boolean isDemonstration() { return demonstration; }
}
