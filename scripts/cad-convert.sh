#!/bin/bash
# CAD 图纸转换脚本示例
# 用法: cad-convert.sh <输入文件路径> <输出PNG路径>
# 依赖: libredwg (dwg2dxf) + 一个 DXF 转 PNG 的工具
# 部署时根据服务器环境修改此脚本，然后在 application.yml 中配置:
# decoboon:
#   cad:
#     convert-script: /path/to/cad-convert.sh
#     temp-dir: /tmp/cad-convert

set -e

INPUT="$1"
OUTPUT="$2"

if [ -z "$INPUT" ] || [ -z "$OUTPUT" ]; then
    echo "用法: $0 <输入文件> <输出PNG>"
    exit 1
fi

WORKDIR=$(dirname "$INPUT")
DXF_FILE="$WORKDIR/convert.dxf"

# 步骤1: DWG → DXF (使用 libredwg)
if command -v dwg2dxf &> /dev/null; then
    dwg2dxf -o "$DXF_FILE" "$INPUT" 2>/dev/null || {
        echo "dwg2dxf 转换失败"
        exit 1
    }
else
    echo "未找到 dwg2dxf，请安装 libredwg"
    exit 1
fi

# 步骤2: DXF → PNG
# 方案A: 使用 dxf2png (如果已安装)
# 方案B: 使用 Python ezdxf + matplotlib
# 方案C: 使用 ODA File Converter 先转 PDF 再转 PNG
# 以下为 Python 方案示例:
if command -v python3 &> /dev/null; then
    python3 - "$DXF_FILE" "$OUTPUT" << 'PYEOF'
import sys
try:
    import ezdxf
    from ezdxf.addons.drawing import RenderContext, Frontend
    from ezdxf.addons.drawing.matplotlib import MatplotlibBackend
    import matplotlib.pyplot as plt
except ImportError:
    print("缺少 ezdxf 或 matplotlib，请运行: pip install ezdxf matplotlib")
    sys.exit(1)

dxf_path = sys.argv[1]
png_path = sys.argv[2]

doc = ezdxf.readfile(dxf_path)
msp = doc.modelspace()

fig = plt.figure(figsize=(20, 14), dpi=100)
ax = fig.add_axes([0, 0, 1, 1])
ax.set_facecolor('white')
ctx = RenderContext(doc)
out = MatplotlibBackend(ax)
Frontend(ctx, out).draw_layout(msp, finalize=True)
fig.savefig(png_path, dpi=100, facecolor='white', bbox_inches='tight', pad_inches=0.1)
plt.close(fig)
PYEOF
else
    echo "未找到 python3，无法转换 DXF 为 PNG"
    exit 1
fi

echo "转换完成: $OUTPUT"
