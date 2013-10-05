package com.karbens.photowidget;

public class RegularSizeWidget_2x4 extends RegularSizeWidgetBase {
	@Override
	public String getUriSchemaId() {
		return "images_widget_2x4";
	}

	@Override
	public String getOnClickAction() {
		return "com.karbens.photowidget.RegularSizeWidget_2x4.onclick";
	}
}
