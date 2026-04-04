package com.iispl.entity;

import java.util.List;

public class NPCI {
	private List<NPCIBank> npciBanksList;

	public NPCI(List<NPCIBank> npciBanksList) {
		this.npciBanksList = npciBanksList;
	}

	public List<NPCIBank> getNpciBanksList() {
		return npciBanksList;
	}

	public void setNpciBanksList(List<NPCIBank> npciBanksList) {
		this.npciBanksList = npciBanksList;
	}
	
	
}
