import {
  Space,
  Button,
  Divider,
  Dropdown,
  Input,
  ColorPicker,
  Tooltip,
  message,
  Popover
} from 'antd'
import type { MenuProps } from 'antd'
import {
  SaveOutlined,
  EyeOutlined,
  RocketOutlined,
  UndoOutlined,
  RedoOutlined,
  AlignLeftOutlined,
  AlignCenterOutlined,
  AlignRightOutlined,
  VerticalAlignTopOutlined,
  VerticalAlignMiddleOutlined,
  VerticalAlignBottomOutlined,
  BgColorsOutlined,
  FontColorsOutlined,
  BorderOutlined,
  BorderlessTableOutlined,
  MergeCellsOutlined,
  ColumnHeightOutlined,
  FormatPainterOutlined,
  FunctionOutlined,
  BarChartOutlined,
  FilterOutlined,
  BoldOutlined,
  ItalicOutlined,
  UnderlineOutlined,
  StrikethroughOutlined,
  PrinterOutlined
} from '@ant-design/icons'
import { useDesignerStore } from '../../store/designer'
import {
  setRangeStyle,
  mergeCells,
  unmergeCells,
  setBorder,
  getSelectedRange,
  undo as luckysheetUndo,
  redo as luckysheetRedo,
  refreshLuckysheet
} from '../../utils/luckysheet'

const FONT_FAMILIES = [
  'Arial',
  'Microsoft YaHei',
  'SimSun',
  'SimHei',
  'KaiTi',
  'FangSong',
  'Times New Roman',
  'Courier New',
  'Verdana',
  'Georgia'
]

const FONT_SIZES = [10, 11, 12, 14, 16, 18, 20, 22, 24, 28, 32, 36, 40, 48, 56, 64, 72]

const Toolbar: React.FC = () => {
  const {
    templateName,
    setTemplateName,
    setExpressionEditorVisible,
    setConditionalFormatVisible,
    setChartConfigVisible,
    cellValue,
    undoStack,
    redoStack
  } = useDesignerStore()

  const handleUndo = () => {
    luckysheetUndo()
  }

  const handleRedo = () => {
    luckysheetRedo()
  }

  const handleSave = () => {
    message.success('保存成功')
  }

  const handlePreview = () => {
    message.info('预览功能')
  }

  const handlePublish = () => {
    message.success('发布成功')
  }

  const applyStyle = (styleKey: string, value: any) => {
    const range = getSelectedRange()
    if (!range) {
      message.warning('请先选择单元格')
      return
    }
    setRangeStyle(range, { [styleKey]: value })
    refreshLuckysheet()
  }

  const handleBold = () => {
    applyStyle('fontWeight', 'bold')
  }

  const handleItalic = () => {
    applyStyle('fontStyle', 'italic')
  }

  const handleUnderline = () => {
    applyStyle('textDecoration', 'underline')
  }

  const handleStrikethrough = () => {
    applyStyle('textDecoration', 'line-through')
  }

  const handleFontFamily: MenuProps['onClick'] = ({ key }) => {
    applyStyle('fontFamily', key)
  }

  const handleFontSize: MenuProps['onClick'] = ({ key }) => {
    applyStyle('fontSize', parseInt(key, 10))
  }

  const handleTextColor = (color: any) => {
    applyStyle('color', color.toHexString())
  }

  const handleBgColor = (color: any) => {
    applyStyle('backgroundColor', color.toHexString())
  }

  const handleAlign = (align: 'left' | 'center' | 'right') => {
    applyStyle('textAlign', align)
  }

  const handleVerticalAlign = (align: 'top' | 'middle' | 'bottom') => {
    applyStyle('verticalAlign', align)
  }

  const handleMergeCells = () => {
    const range = getSelectedRange()
    if (!range) {
      message.warning('请先选择要合并的单元格区域')
      return
    }
    if (range.start.row === range.end.row && range.start.col === range.end.col) {
      message.warning('请选择多个单元格进行合并')
      return
    }
    mergeCells(range)
    message.success('合并成功')
  }

  const handleUnmergeCells = () => {
    const range = getSelectedRange()
    if (!range) {
      message.warning('请先选择单元格')
      return
    }
    unmergeCells(range.start.row, range.start.col)
    message.success('取消合并成功')
  }

  const handleBorder: MenuProps['onClick'] = ({ key }) => {
    const range = getSelectedRange()
    if (!range) {
      message.warning('请先选择单元格')
      return
    }
    setBorder(range, key as any)
  }

  const handleOpenExpressionEditor = () => {
    setExpressionEditorVisible(true, cellValue)
  }

  const handleOpenConditionalFormat = () => {
    setConditionalFormatVisible(true)
  }

  const handleOpenChartConfig = () => {
    setChartConfigVisible(true)
  }

  const fontFamilyMenu: MenuProps = {
    onClick: handleFontFamily,
    items: FONT_FAMILIES.map((f) => ({
      key: f,
      label: <span style={{ fontFamily: f }}>{f}</span>
    }))
  }

  const fontSizeMenu: MenuProps = {
    onClick: handleFontSize,
    items: FONT_SIZES.map((s) => ({
      key: String(s),
      label: `${s}px`
    }))
  }

  const borderMenu: MenuProps = {
    onClick: handleBorder,
    items: [
      { key: 'all', label: '所有边框', icon: <BorderOutlined /> },
      { key: 'outer', label: '外边框' },
      { key: 'inner', label: '内边框' },
      { type: 'divider' as const },
      { key: 'top', label: '上边框' },
      { key: 'bottom', label: '下边框' },
      { key: 'left', label: '左边框' },
      { key: 'right', label: '右边框' },
      { type: 'divider' as const },
      { key: 'none', label: '无边框', icon: <BorderlessTableOutlined /> }
    ]
  }

  return (
    <div
      style={{
        padding: '8px 12px',
        borderBottom: '1px solid #f0f0f0',
        background: '#fff',
        display: 'flex',
        alignItems: 'center',
        gap: 8
      }}
    >
      <Space size="small">
        <Input
          value={templateName}
          onChange={(e) => setTemplateName(e.target.value)}
          style={{ width: 180 }}
          size="small"
          prefix={<span style={{ color: '#999' }}>报表：</span>}
        />
      </Space>

      <Divider type="vertical" />

      <Space size={4}>
        <Tooltip title="保存">
          <Button size="small" icon={<SaveOutlined />} onClick={handleSave} type="primary">
            保存
          </Button>
        </Tooltip>
        <Tooltip title="预览">
          <Button size="small" icon={<EyeOutlined />} onClick={handlePreview}>
            预览
          </Button>
        </Tooltip>
        <Tooltip title="发布">
          <Button size="small" icon={<RocketOutlined />} onClick={handlePublish}>
            发布
          </Button>
        </Tooltip>
        <Tooltip title="打印">
          <Button size="small" icon={<PrinterOutlined />} />
        </Tooltip>
      </Space>

      <Divider type="vertical" />

      <Space size={2}>
        <Tooltip title="撤销">
          <Button
            size="small"
            icon={<UndoOutlined />}
            onClick={handleUndo}
            disabled={undoStack.length === 0}
          />
        </Tooltip>
        <Tooltip title="重做">
          <Button
            size="small"
            icon={<RedoOutlined />}
            onClick={handleRedo}
            disabled={redoStack.length === 0}
          />
        </Tooltip>
        <Tooltip title="格式刷">
          <Button size="small" icon={<FormatPainterOutlined />} />
        </Tooltip>
      </Space>

      <Divider type="vertical" />

      <Space size={2}>
        <Dropdown menu={fontFamilyMenu}>
          <Button size="small">
            字体 <span style={{ marginLeft: 4 }}>▼</span>
          </Button>
        </Dropdown>
        <Dropdown menu={fontSizeMenu}>
          <Button size="small" icon={<ColumnHeightOutlined />}>
            字号
          </Button>
        </Dropdown>
      </Space>

      <Divider type="vertical" />

      <Space size={2}>
        <Tooltip title="加粗">
          <Button size="small" onClick={handleBold} style={{ fontWeight: 'bold' }}>
            <BoldOutlined />
          </Button>
        </Tooltip>
        <Tooltip title="斜体">
          <Button size="small" onClick={handleItalic} style={{ fontStyle: 'italic' }}>
            <ItalicOutlined />
          </Button>
        </Tooltip>
        <Tooltip title="下划线">
          <Button size="small" onClick={handleUnderline} style={{ textDecoration: 'underline' }}>
            <UnderlineOutlined />
          </Button>
        </Tooltip>
        <Tooltip title="删除线">
          <Button size="small" onClick={handleStrikethrough} style={{ textDecoration: 'line-through' }}>
            <StrikethroughOutlined />
          </Button>
        </Tooltip>
      </Space>

      <Divider type="vertical" />

      <Space size={2}>
        <Popover
          content={
            <ColorPicker
              defaultValue="#000000"
              onChange={handleTextColor}
              showText
            />
          }
          title="文字颜色"
          trigger="click"
        >
          <Tooltip title="文字颜色">
            <Button size="small" icon={<FontColorsOutlined />} />
          </Tooltip>
        </Popover>
        <Popover
          content={
            <ColorPicker
              defaultValue="#ffffff"
              onChange={handleBgColor}
              showText
            />
          }
          title="背景颜色"
          trigger="click"
        >
          <Tooltip title="背景颜色">
            <Button size="small" icon={<BgColorsOutlined />} />
          </Tooltip>
        </Popover>
      </Space>

      <Divider type="vertical" />

      <Space size={2}>
        <Tooltip title="左对齐">
          <Button size="small" icon={<AlignLeftOutlined />} onClick={() => handleAlign('left')} />
        </Tooltip>
        <Tooltip title="居中对齐">
          <Button size="small" icon={<AlignCenterOutlined />} onClick={() => handleAlign('center')} />
        </Tooltip>
        <Tooltip title="右对齐">
          <Button size="small" icon={<AlignRightOutlined />} onClick={() => handleAlign('right')} />
        </Tooltip>
      </Space>

      <Divider type="vertical" />

      <Space size={2}>
        <Tooltip title="顶端对齐">
          <Button size="small" icon={<VerticalAlignTopOutlined />} onClick={() => handleVerticalAlign('top')} />
        </Tooltip>
        <Tooltip title="垂直居中">
          <Button size="small" icon={<VerticalAlignMiddleOutlined />} onClick={() => handleVerticalAlign('middle')} />
        </Tooltip>
        <Tooltip title="底端对齐">
          <Button size="small" icon={<VerticalAlignBottomOutlined />} onClick={() => handleVerticalAlign('bottom')} />
        </Tooltip>
      </Space>

      <Divider type="vertical" />

      <Space size={2}>
        <Tooltip title="合并单元格">
          <Button size="small" icon={<MergeCellsOutlined />} onClick={handleMergeCells} />
        </Tooltip>
        <Tooltip title="取消合并">
          <Button size="small" icon={<BorderlessTableOutlined />} onClick={handleUnmergeCells} />
        </Tooltip>
        <Dropdown menu={borderMenu}>
          <Tooltip title="边框">
            <Button size="small" icon={<BorderOutlined />} />
          </Tooltip>
        </Dropdown>
      </Space>

      <Divider type="vertical" />

      <Space size={2}>
        <Tooltip title="表达式">
          <Button size="small" icon={<FunctionOutlined />} onClick={handleOpenExpressionEditor}>
            fx
          </Button>
        </Tooltip>
        <Tooltip title="条件格式">
          <Button size="small" icon={<FilterOutlined />} onClick={handleOpenConditionalFormat}>
            条件格式
          </Button>
        </Tooltip>
        <Tooltip title="插入图表">
          <Button size="small" icon={<BarChartOutlined />} onClick={handleOpenChartConfig}>
            图表
          </Button>
        </Tooltip>
      </Space>
    </div>
  )
}

export default Toolbar
