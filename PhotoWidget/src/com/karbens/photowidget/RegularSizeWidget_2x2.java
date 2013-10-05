package com.karbens.photowidget;

public class RegularSizeWidget_2x2 extends RegularSizeWidgetBase {
	@Override
	public String getUriSchemaId() {
		return "images_widget_2x2";
	}

	@Override
	public String getOnClickAction() {
		return "com.karbens.photowidget.RegularSizeWidget_2x2.onclick";
	}
}
