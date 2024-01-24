package com.github.hhhzzzsss.epsilonbot.mapart;

import java.awt.*;

public class BlockElevation {
	public String block;
	public int tone;
	public Color color;
	public int elevation;
	
	public BlockElevation(String block, int tone, Color color) {
		this.block = block;
		this.tone = tone;
		this.color = color;
		this.elevation = 0;
	}
}
