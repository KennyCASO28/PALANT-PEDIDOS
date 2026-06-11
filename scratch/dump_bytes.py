
with open(r"c:\Users\palan\Desktop\PALANT-PEDIDOS\src\main\java\org\example\component\TextLayer.java", "rb") as f:
    f.seek(11000) # Seek to roughly where line 354 is
    chunk = f.read(2000)
    print(chunk)
