
with open(r"c:\Users\palan\Desktop\PALANT-PEDIDOS\src\main\java\org\example\component\TextLayer.java", "r", encoding="utf-8") as f:
    content = f.read()
    open_p = content.count("(")
    close_p = content.count(")")
    print(f"Open P: {open_p}, Close P: {close_p}")
