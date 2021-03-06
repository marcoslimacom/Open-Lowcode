/********************************************************************************
 * Copyright (c) 2019-2020 [Open Lowcode SAS](https://openlowcode.com/)
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0 .
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.openlowcode.design.pages;

import java.io.IOException;

import org.openlowcode.design.data.ArgumentContent;
import org.openlowcode.design.data.DataObjectDefinition;
import org.openlowcode.design.data.argument.ArrayArgument;
import org.openlowcode.design.data.argument.NodeTreeArgument;
import org.openlowcode.design.data.argument.ObjectArgument;
import org.openlowcode.design.generation.SourceGenerator;
import org.openlowcode.design.generation.StringFormatter;
import org.openlowcode.design.module.Module;
import org.openlowcode.tools.misc.Named;
import org.openlowcode.tools.misc.NamedList;

/**
 * A page definition details the input attributes of a page, and will generate
 * an abstract page class to be derived to provide actual layout
 * 
 * @author <a href="https://openlowcode.com/" rel="nofollow">Open Lowcode
 *         SAS</a>
 *
 */
public abstract class PageDefinition
		extends
		Named {

	@SuppressWarnings("unused")
	private AddonPageDefinition addon;
	private boolean noaddon = false;
	private NamedList<ArgumentContent> emptyattributelist;
	private boolean autogenerated;

	/**
	 * @return true if the page is auto-generated
	 */
	public boolean isAutogenerated() {
		return autogenerated;
	}

	/**
	 * This method should be overriden by page types that allow inputattributes
	 * 
	 * @return the list of input attributes
	 */
	public NamedList<ArgumentContent> getPageAttributes() {
		return emptyattributelist;
	}

	/**
	 * @return the class add-on (added to SPage in the import statement)
	 */
	public String getClassAddon() {
		return "";
	}

	/**
	 * @return the attribute method-add-on
	 */
	public String getAttributeMethodAddon() {
		return "";
	}

	/**
	 * creates a page definition that is not automatically generated
	 * 
	 * @param name unique name of the page (should be a valid java name)
	 */
	public PageDefinition(String name) {
		super(name);
		emptyattributelist = new NamedList<ArgumentContent>();
		addon = null;
		this.autogenerated = false;
	}

	/**
	 * creating a page definition
	 * 
	 * @param name          unique name of the page (should be a valid java name)
	 * @param autogenerated true if class is auto-generated
	 */
	public PageDefinition(String name, boolean autogenerated) {
		this(name);
		this.autogenerated = autogenerated;
	}

	/**
	 * speficic that the page should have a specific add-on, not the default one of
	 * the Open Lowcode application
	 * 
	 * @param addon
	 */
	public void setSpecificAddon(AddonPageDefinition addon) {
		this.addon = addon;
	}

	/**
	 * defines that the page should not use any add-on
	 */
	public void setNoAddOn() {
		this.noaddon = true;
	}

	/**
	 * generates the abstract class for the page
	 * 
	 * @param sg     source generator
	 * @param module parent module
	 * @throws IOException if anything happens while writing the file
	 */
	public void generateToFile(SourceGenerator sg, Module module) throws IOException {
		String classname = StringFormatter.formatForJavaClass(this.getName());
		sg.wl("package " + module.getPath() + ".page.generated;");

		if (!this.isAutogenerated()) {
			sg.wl("/* <<<< Warning >>>> : You need to implement a concrete class in a separate file *!*! -----");
			sg.wl(" > This is an automatically generated abstract class providing the interface to ");
			sg.wl(" > the page you have to implement. Please create the class implementing ");
			sg.wl(" > this abstract class at path: ");
			sg.wl("    --> " + module.getPath() + ".action." + StringFormatter.formatForJavaClass(this.getName())
					+ "Page");
			sg.wl(" > Please do NOT modify the current file, as it will be erased and generated again.");
			sg.wl(" ----------------------------------------------------------------------------------------- */");
		}

		sg.wl("import org.openlowcode.server.graphic.SPage" + this.getClassAddon() + ";");
		sg.wl("import org.openlowcode.server.graphic.SPageData;");
		sg.wl("import org.openlowcode.server.graphic.SPageNode;");
		sg.wl("import org.openlowcode.server.runtime.OLcServer;");
		sg.wl("import org.openlowcode.tools.structure.TextDataElt;");
		sg.wl("import org.openlowcode.server.data.message.*;");
		sg.wl("import org.openlowcode.tools.structure.*;");

		sg.wl("import " + module.getPath() + ".data.*;");
		sg.wl("import " + module.getPath() + ".page.*;");
		for (int i = 0; i < this.getPageAttributes().getSize(); i++) {
			ArgumentContent attribute = this.getPageAttributes().get(i);
			this.getPageAttributes().get(i).writeImports(sg, module);
			if (attribute instanceof ArrayArgument) {
				attribute = ((ArrayArgument) attribute).getPayload();

			}
			if (attribute instanceof ObjectArgument) {
				ObjectArgument objectargument = (ObjectArgument) attribute;
				DataObjectDefinition objectdef = objectargument.getPayload();
				// object from different module, write import
				if (objectdef.getOwnermodule().getPath().compareTo(module.getPath()) != 0) {
					sg.wl("import " + objectdef.getOwnermodule().getPath() + ".data."
							+ StringFormatter.formatForJavaClass(objectdef.getName()) + ";");
				}
			}

		}

		sg.wl("		public abstract class Abs" + classname + "Page extends SPage" + this.getClassAddon() + " {");
		sg.wl("			private SPageData pagedata;");
		for (int i = 0; i < this.getPageAttributes().getSize(); i++) {
			ArgumentContent thisarg = this.getPageAttributes().get(i);
			sg.wl("			private " + thisarg.getPreciseDataEltName() + " "
					+ StringFormatter.formatForAttribute(thisarg.getName()) + ";");
		}
		sg.bl();
		for (int i = 0; i < this.getPageAttributes().getSize(); i++) {
			ArgumentContent thisarg = this.getPageAttributes().get(i);
			sg.wl("			public " + thisarg.getPreciseDataEltName() + " get"
					+ StringFormatter.formatForJavaClass(thisarg.getName()) + "() {");
			sg.wl("				return this." + StringFormatter.formatForAttribute(thisarg.getName()) + ";");
			sg.wl("			}");
			sg.bl();
		}

		// addition of generate title abstract method

		sg.w("			public abstract String generateTitle(");
		for (int i = 0; i < this.getPageAttributes().getSize(); i++) {
			ArgumentContent thisarg = this.getPageAttributes().get(i);
			if (i > 0)
				sg.w(",");
			sg.w("	" + thisarg.getType() + " " + StringFormatter.formatForAttribute(thisarg.getName()));

		}
		sg.wl(") ;");

		sg.w("			public Abs" + classname + "Page(");
		for (int i = 0; i < this.getPageAttributes().getSize(); i++) {
			ArgumentContent thisarg = this.getPageAttributes().get(i);
			if (i > 0)
				sg.w(",");
			sg.w("	" + thisarg.getType() + " " + StringFormatter.formatForAttribute(thisarg.getName()));

		}
		sg.wl(")  {");
		sg.wl("				super(\"" + this.getName() + "\");");
		sg.w("				this.setTitle(generateTitle(");
		for (int i = 0; i < this.getPageAttributes().getSize(); i++) {
			ArgumentContent thisarg = this.getPageAttributes().get(i);
			if (i > 0)
				sg.w(",");
			sg.w("	" + StringFormatter.formatForAttribute(thisarg.getName()));

		}
		sg.wl("));");
		sg.wl("				pagedata = new SPageData();");
		if (!(this instanceof AddonPageDefinition))
			if (!this.noaddon)
				sg.wl("				this.addAddon(OLcServer.getServer().getMainmodule().getPageAddonForModule());");

		for (int i = 0; i < this.getPageAttributes().getSize(); i++) {
			ArgumentContent thisarg = this.getPageAttributes().get(i);

			if ((!(thisarg instanceof NodeTreeArgument)) && (!(thisarg instanceof ArrayArgument))) {
				sg.wl("				this." + StringFormatter.formatForAttribute(thisarg.getName()) + " = new "
						+ thisarg.getPreciseDataEltName() + "(\"" + thisarg.getName() + "\","
						+ StringFormatter.formatForAttribute(thisarg.getName()) + ");");
				sg.wl("				pagedata.addDataElt(this." + StringFormatter.formatForAttribute(thisarg.getName())
						+ ");");
			}
			if (thisarg instanceof NodeTreeArgument) {
				// NEED TO ADD A LOT OF STUFF SO THAT IT WORKS CORRECT
				sg.wl("			this." + StringFormatter.formatForAttribute(thisarg.getName()) + " = "
						+ thisarg.getName().toLowerCase() + ".generateObjectTreeDataElt(\"" + thisarg.getName()
						+ "\");");
				sg.wl("			pagedata.addDataElt(this." + StringFormatter.formatForAttribute(thisarg.getName())
						+ ");");
			}
			if (thisarg instanceof ArrayArgument) {
				ArrayArgument arrayargument = (ArrayArgument) thisarg;
				sg.wl("			this." + StringFormatter.formatForAttribute(thisarg.getName()) + " = new  ArrayDataElt<"
						+ arrayargument.getPayload().getPreciseDataEltName() + ">(\"" + arrayargument.getName()
						+ "\",new " + arrayargument.getPayload().getPreciseDataEltTypeNameWithArgument() + ");");
				sg.wl("			for (int i=0;i<" + StringFormatter.formatForAttribute(thisarg.getName())
						+ ".length;i++) {");
				sg.wl("				this." + StringFormatter.formatForAttribute(thisarg.getName()) + ".addElement(new "
						+ arrayargument.getPayload().getPreciseDataEltName() + "(\"" + arrayargument.getName() + "\","
						+ StringFormatter.formatForAttribute(thisarg.getName()) + "[i]));");
				sg.wl("			}");
				sg.wl("			pagedata.addDataElt(this." + StringFormatter.formatForAttribute(thisarg.getName())
						+ ");");
			}
		}

		sg.wl("			}");

		sg.wl("			@Override");
		sg.wl("			public SPageData getAll" + this.getAttributeMethodAddon() + "PageAttributes() {");
		sg.wl("				return this.pagedata;");
		sg.wl("			}");

		sg.wl("		}");
		sg.close();

	}

}
