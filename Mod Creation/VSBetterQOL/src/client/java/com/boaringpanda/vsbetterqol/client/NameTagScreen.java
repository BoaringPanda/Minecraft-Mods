package com.boaringpanda.vsbetterqol.client;

import com.boaringpanda.vsbetterqol.NameTagRenaming.RenamePayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;

// Opened by right-clicking with a name tag. Accept sends the new name to the server, which renames the tag and uses an ink sac.
public class NameTagScreen extends Screen {
	private final InteractionHand hand;
	private final String oldName;
	private EditBox nameEdit;
	private Button acceptButton;

	public NameTagScreen(InteractionHand hand, ItemStack nameTag) {
		super(Component.translatable("gui.vsbetterqol.name_tag"));
		this.hand = hand;
		Component customName = nameTag.get(DataComponents.CUSTOM_NAME);
		this.oldName = customName == null ? "" : customName.getString();
	}

	@Override
	protected void init() {
		this.nameEdit = new EditBox(this.font, this.width / 2 - 100, 116, 200, 20, this.title);
		this.nameEdit.setMaxLength(AnvilMenu.MAX_NAME_LENGTH);
		this.nameEdit.setValue(this.oldName);
		this.nameEdit.setResponder(value -> this.updateAcceptButtonStatus());
		this.addWidget(this.nameEdit);
		this.acceptButton = this.addRenderableWidget(
				Button.builder(Component.translatable("gui.vsbetterqol.name_tag.accept"), button -> this.onAccept())
						.bounds(this.width / 2 - 100, this.height / 4 + 96 + 12, 200, 20)
						.build());
		this.addRenderableWidget(
				Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose())
						.bounds(this.width / 2 - 100, this.height / 4 + 120 + 12, 200, 20)
						.build());
		this.updateAcceptButtonStatus();
	}

	@Override
	protected void setInitialFocus() {
		this.setInitialFocus(this.nameEdit);
	}

	@Override
	public void resize(final int width, final int height) {
		String oldEdit = this.nameEdit.getValue();
		this.init(width, height);
		this.nameEdit.setValue(oldEdit);
	}

	@Override
	public boolean keyPressed(final KeyEvent event) {
		if (this.acceptButton.active && this.getFocused() == this.nameEdit && event.isConfirmation()) {
			this.onAccept();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	// Blank or unchanged names can't be accepted, so an ink sac is never used for nothing.
	private void updateAcceptButtonStatus() {
		String name = StringUtil.filterText(this.nameEdit.getValue());
		this.acceptButton.active = !StringUtil.isBlank(name) && !name.equals(this.oldName);
	}

	private void onAccept() {
		ClientPlayNetworking.send(new RenamePayload(this.hand, this.nameEdit.getValue()));
		this.onClose();
	}

	@Override
	public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);
		graphics.centeredText(this.font, this.title, this.width / 2, 20, -1);
		this.nameEdit.extractRenderState(graphics, mouseX, mouseY, a);
	}
}
