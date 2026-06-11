
with open(r"c:\Users\palan\Desktop\PALANT-PEDIDOS\src\main\java\org\example\component\TextLayer.java", "r", encoding="utf-8") as f:
    content = f.read()
    open_b = content.count("[")
    close_b = content.count("]")
    print(f"Open B: {open_b}, Close B: {close_b}")
