package com.github.hhhzzzsss.epsilonbot.block;

import lombok.Data;
import org.cloudburstmc.math.vector.Vector3i;

@Data
public class BlockChangeInfo {
	private final Vector3i position;
	private final int newId;
}
