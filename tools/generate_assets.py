#!/usr/bin/env python3
"""
Generates the castle structure (NBT) and the two key textures for Paradox Castle.

    python3 tools/generate_assets.py

Outputs:
  src/main/resources/data/paradoxcastle/structure/paradox_castle.nbt
  src/main/resources/assets/paradoxcastle/textures/item/koraidon_key.png
  src/main/resources/assets/paradoxcastle/textures/item/miraidon_key.png

No third-party packages required.
"""
import gzip
import math
import os
import random
import struct
import zlib

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "src/main/resources")

# --------------------------------------------------------------------------
# Minimal NBT writer
# --------------------------------------------------------------------------
IDS = {"byte": 1, "int": 3, "str": 8, "list": 9, "compound": 10}


def _s(text):
    b = text.encode("utf-8")
    return struct.pack(">H", len(b)) + b


def _payload(kind, val):
    if kind == "int":
        return struct.pack(">i", val)
    if kind == "str":
        return _s(val)
    if kind == "compound":
        out = b""
        for key, (k, v) in val.items():
            out += bytes([IDS[k]]) + _s(key) + _payload(k, v)
        return out + b"\x00"
    if kind == "list":
        elem, items = val
        return bytes([IDS[elem]]) + struct.pack(">i", len(items)) + b"".join(
            _payload(elem, i) for i in items
        )
    raise ValueError(kind)


def write_nbt(path, compound):
    data = bytes([10]) + _s("") + _payload("compound", compound)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with gzip.GzipFile(path, "wb", mtime=0) as fh:
        fh.write(data)


# --------------------------------------------------------------------------
# Castle model
# --------------------------------------------------------------------------
W, H, D = 43, 34, 43          # template size (x, y, z)
FLOOR = 5                      # y of the courtyard floor layer
rnd = random.Random(20261015)
blocks = {}                    # (x,y,z) -> (name, props tuple, nbt dict | None)


def put(x, y, z, name, props=None, nbt=None):
    if 0 <= x < W and 0 <= y < H and 0 <= z < D:
        blocks[(x, y, z)] = (name, tuple(sorted((props or {}).items())), nbt)


def air(x, y, z):
    put(x, y, z, "minecraft:air")


def fill(x1, y1, z1, x2, y2, z2, name, props=None):
    for x in range(min(x1, x2), max(x1, x2) + 1):
        for y in range(min(y1, y2), max(y1, y2) + 1):
            for z in range(min(z1, z2), max(z1, z2) + 1):
                put(x, y, z, name, props)


def brick():
    r = rnd.random()
    if r < 0.70:
        return "minecraft:stone_bricks"
    if r < 0.85:
        return "minecraft:mossy_stone_bricks"
    return "minecraft:cracked_stone_bricks"


def pbrick(x, y, z):
    put(x, y, z, brick())


def stairs(x, y, z, kind, facing):
    put(x, y, z, kind, {"facing": facing, "half": "bottom", "shape": "straight"})


def chest(x, y, z, facing, loot):
    put(x, y, z, "minecraft:chest", {"facing": facing, "type": "single"},
        {"id": ("str", "minecraft:chest"), "LootTable": ("str", loot)})


def hanging_lantern(x, y, z):
    put(x, y, z, "minecraft:lantern", {"hanging": "true"})


FILLER = "paradoxcastle:chests/castle_filler"
KORAIDON_LOOT = "paradoxcastle:chests/koraidon_key"
MIRAIDON_LOOT = "paradoxcastle:chests/miraidon_key"

# 1. clear the whole footprint above the floor so hills/trees do not intrude
fill(2, FLOOR + 1, 2, 40, H - 1, 40, "minecraft:air")

# 2. foundation + courtyard floor
for x in range(2, 41):
    for z in range(2, 41):
        for y in range(0, FLOOR):
            put(x, y, z, "minecraft:cobblestone" if rnd.random() < 0.5 else "minecraft:stone_bricks")
        pbrick(x, FLOOR, z)

# 3. curtain wall (2 thick, ring 4..38)
for x in range(4, 39):
    for z in range(4, 39):
        inner_void = 6 <= x <= 36 and 6 <= z <= 36
        if inner_void:
            continue
        for y in range(FLOOR + 1, 13):
            pbrick(x, y, z)
        outer = x in (4, 38) or z in (4, 38)
        if outer:
            pbrick(x, 13, z)
            if (x + z) % 2 == 0:
                pbrick(x, 14, z)

# 4. gatehouse opening (south wall) + portcullis + path
fill(19, 6, 37, 23, 10, 38, "minecraft:air")
for x in range(19, 24):
    put(x, 10, 38, "minecraft:iron_bars")
for x in range(20, 23):
    for z in range(30, 41):
        put(x, FLOOR, z, "minecraft:polished_andesite")

# 5. corner towers
towers = [  # (cx, cz, loot)
    (6, 6, KORAIDON_LOOT),
    (36, 36, MIRAIDON_LOOT),
    (36, 6, FILLER),
    (6, 36, FILLER),
]
for cx, cz, loot in towers:
    ox = -1 if cx < 21 else 1
    oz = -1 if cz < 21 else 1

    def T(dx, dz):
        return cx + dx, cz + dz

    # shell + hollow interior
    for dx in range(-4, 5):
        for dz in range(-4, 5):
            cheb = max(abs(dx), abs(dz))
            chamfer = abs(dx) == 4 and abs(dz) == 4
            x, z = T(dx, dz)
            for y in range(FLOOR + 1, 22):
                if cheb == 4 and not chamfer:
                    pbrick(x, y, z)
                elif cheb < 4:
                    air(x, y, z)
    # wooden floors at y=12 and y=18 (aligned with the wall walkway)
    for dx in range(-3, 4):
        for dz in range(-3, 4):
            for y in (12, 18):
                x, z = T(dx, dz)
                put(x, y, z, "minecraft:spruce_planks")
    # ladder shaft against the outer wall
    lx, lz = T(ox * 3, oz * 2)
    for y in (12, 18):
        air(lx, y, lz)
    for y in range(6, 20):
        put(lx, y, lz, "minecraft:ladder", {"facing": "east" if ox == -1 else "west"})
    # courtyard door (ground floor, faces the castle centre)
    for y in (6, 7):
        x, z = T(-ox * 4, 0)
        air(x, y, z)
    # wall-walkway doors
    for y in (13, 14):
        x, z = T(-ox * 4, oz * 1)
        air(x, y, z)
        x, z = T(ox * 1, -oz * 4)
        air(x, y, z)
    # arrow slits
    for (dx, dz) in ((ox * 4, 0), (0, oz * 4)):
        x, z = T(dx, dz)
        air(x, 9, z)
        air(x, 15, z)
    # lighting
    for y in (11, 17, 21):
        x, z = T(0, 0)
        hanging_lantern(x, y, z)
    # roof: stepped pyramid + spire
    for k in range(5):
        h = 4 - k
        for dx in range(-h, h + 1):
            for dz in range(-h, h + 1):
                if k == 0 and abs(dx) == 4 and abs(dz) == 4:
                    continue
                x, z = T(dx, dz)
                put(x, 22 + k, z, "minecraft:deepslate_tiles")
    x, z = T(0, 0)
    put(x, 27, z, "minecraft:iron_bars")
    put(x, 28, z, "minecraft:iron_bars")
    # chest in the top room
    x, z = T(-ox * 3, 0)
    chest(x, 19, z, "west" if ox == -1 else "east", loot)

# 6. central keep (x,z 13..29)
for x in range(13, 30):
    for z in range(13, 30):
        edge = x in (13, 29) or z in (13, 29)
        if edge:
            for y in range(FLOOR + 1, 27):
                pbrick(x, y, z)
        else:
            for y in range(FLOOR + 1, 26):
                air(x, y, z)
# corner pillars
for x, z in ((13, 13), (13, 29), (29, 13), (29, 29)):
    for y in range(FLOOR + 1, 29):
        put(x, y, z, "minecraft:deepslate_bricks")
    put(x, 29, z, "minecraft:lantern")
# floors (y=12 and y=19) and roof deck (y=26)
for x in range(14, 29):
    for z in range(14, 29):
        put(x, 12, z, "minecraft:dark_oak_planks")
        put(x, 19, z, "minecraft:dark_oak_planks")
for x in range(13, 30):
    for z in range(13, 30):
        pbrick(x, 26, z)
# parapet + merlons on the roof
for x in range(13, 30):
    for z in range(13, 30):
        if x in (13, 29) or z in (13, 29):
            pbrick(x, 27, z)
            if (x + z) % 2 == 0:
                pbrick(x, 28, z)
# three stair runs (stair "facing" = direction of ascent)
for i in range(7):                       # ground -> 2nd floor, west wall, ascend +z
    stairs(14, 6 + i, 15 + i, "minecraft:stone_brick_stairs", "south")
for z in range(15, 21):
    air(14, 12, z)
for i in range(7):                       # 2nd -> 3rd floor, east wall, ascend -z
    stairs(28, 13 + i, 27 - i, "minecraft:stone_brick_stairs", "north")
for z in range(22, 28):
    air(28, 19, z)
for i in range(7):                       # 3rd floor -> roof, west wall, ascend +z
    stairs(14, 20 + i, 15 + i, "minecraft:stone_brick_stairs", "south")
for z in range(15, 21):
    air(14, 26, z)
# main entrance (south) and windows
for y in range(6, 10):
    fill(20, y, 29, 22, y, 29, "minecraft:air")
for level_y in (8, 15, 22):
    for wx in (16, 26):
        glass = "minecraft:red_stained_glass" if wx < 21 else "minecraft:purple_stained_glass"
        for y in range(level_y, level_y + 3):
            put(wx, y, 13, glass)
            put(wx, y, 29, glass)
    for wz in (17, 25):
        for y in range(level_y, level_y + 3):
            put(13, y, wz, "minecraft:red_stained_glass")
            put(29, y, wz, "minecraft:purple_stained_glass")
# great hall: pillars, lanterns, carpets, dais with the two pedestals
for px in (17, 25):
    for pz in (20, 26):
        for y in range(6, 12):
            put(px, y, pz, "minecraft:quartz_pillar", {"axis": "y"})
for lx, lz in ((16, 20), (26, 20), (16, 26), (26, 26), (21, 22)):
    hanging_lantern(lx, 11, lz)
for z in range(19, 29):
    put(21, 6, z, "minecraft:black_carpet")
for z in range(19, 23):
    put(18, 6, z, "minecraft:red_carpet")
    put(24, 6, z, "minecraft:purple_carpet")
fill(16, 6, 15, 26, 6, 17, "minecraft:polished_blackstone_bricks")
for x in range(17, 26):
    stairs(x, 6, 18, "minecraft:polished_blackstone_brick_stairs", "north")
put(18, 7, 16, "paradoxcastle:koraidon_pedestal")
put(24, 7, 16, "paradoxcastle:miraidon_pedestal")
put(21, 7, 16, "minecraft:chiseled_quartz_block")
put(16, 7, 15, "minecraft:lantern")
put(26, 7, 15, "minecraft:lantern")
# upper floors: lighting + chests
for lx, lz in ((18, 18), (24, 18), (18, 24), (24, 24)):
    hanging_lantern(lx, 18, lz)
    hanging_lantern(lx, 25, lz)
chest(21, 13, 14, "south", FILLER)
chest(21, 20, 14, "south", FILLER)

# 7. courtyard lamp posts
for px, pz in ((19, 33), (23, 33), (19, 31), (23, 31)):
    put(px, 6, pz, "minecraft:oak_fence")
    put(px, 7, pz, "minecraft:oak_fence")
    put(px, 8, pz, "minecraft:lantern")

# --------------------------------------------------------------------------
# Serialise structure
# --------------------------------------------------------------------------
palette, pal_index, block_list = [], {}, []
for (x, y, z), (name, props, nbt) in sorted(blocks.items()):
    key = (name, props)
    if key not in pal_index:
        pal_index[key] = len(palette)
        entry = {"Name": ("str", name)}
        if props:
            entry["Properties"] = ("compound", {k: ("str", v) for k, v in props})
        palette.append(entry)
    b = {"pos": ("list", ("int", [x, y, z])), "state": ("int", pal_index[key])}
    if nbt:
        b["nbt"] = ("compound", nbt)
    block_list.append(b)

root = {
    "DataVersion": ("int", 3955),  # Minecraft 1.21.1
    "size": ("list", ("int", [W, H, D])),
    "palette": ("list", ("compound", palette)),
    "blocks": ("list", ("compound", block_list)),
    "entities": ("list", ("compound", [])),
}
out = os.path.join(RES, "data/paradoxcastle/structure/paradox_castle.nbt")
write_nbt(out, root)
print("castle:", len(block_list), "blocks,", len(palette), "palette entries ->", out)

# --------------------------------------------------------------------------
# Key textures (16x16 PNG, no dependencies)
# --------------------------------------------------------------------------


def write_png(path, pixels):
    raw = b"".join(
        b"\x00" + b"".join(bytes(pixels[y][x]) for x in range(16)) for y in range(16)
    )

    def chunk(tag, data):
        c = struct.pack(">I", len(data)) + tag + data
        return c + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(raw, 9))
    png += chunk(b"IEND", b"")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as fh:
        fh.write(png)


def make_key(ring, shaft, gem, path):
    clear = (0, 0, 0, 0)
    outline = (30, 24, 34, 255)
    mask = {}
    # ring (top-left)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 4.5, y - 4.5)
            if 1.7 <= d <= 3.9:
                mask[(x, y)] = ring
    # shaft (diagonal towards bottom-right)
    for t in range(6, 14):
        for w in (0, 1):
            mask[(t + w, t)] = shaft
    # teeth
    for p in ((11, 13), (12, 13), (13, 11), (13, 12), (9, 11)):
        mask[p] = shaft
    mask[(7, 7)] = gem
    mask[(8, 8)] = gem
    pix = [[clear] * 16 for _ in range(16)]
    for (x, y), c in mask.items():
        pix[y][x] = c + (255,)
    for y in range(16):
        for x in range(16):
            if pix[y][x] == clear:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if (nx, ny) in mask:
                        pix[y][x] = outline
                        break
    write_png(path, pix)


tex = os.path.join(RES, "assets/paradoxcastle/textures/item")
make_key((214, 69, 52), (240, 190, 60), (255, 120, 40), os.path.join(tex, "koraidon_key.png"))
make_key((140, 70, 220), (120, 225, 245), (255, 80, 220), os.path.join(tex, "miraidon_key.png"))
print("textures written")
