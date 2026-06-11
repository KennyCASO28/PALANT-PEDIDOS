
with open(r"c:\Users\palan\Desktop\PALANT-PEDIDOS\src\main\java\org\example\component\TextLayer.java", "r", encoding="utf-8") as f:
    content = f.read()
    open_braces = content.count("{")
    close_braces = content.count("}")
    print(f"Open: {open_braces}, Close: {close_braces}")
