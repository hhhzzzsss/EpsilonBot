package com.github.hhhzzzsss.epsilonbot.mapart;

import com.github.hhhzzzsss.epsilonbot.buildsync.PlotCoord;
import com.google.gson.*;

import java.awt.*;
import java.lang.reflect.Type;

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
