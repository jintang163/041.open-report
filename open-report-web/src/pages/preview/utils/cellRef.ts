export const columnIndexToLetter = (index: number): string => {
  let result = ''
  let temp = index
  while (temp >= 0) {
    result = String.fromCharCode(65 + (temp % 26)) + result
    temp = Math.floor(temp / 26) - 1
  }
  return result
}

export const getCellRef = (rowIndex: number, colIndex: number): string => {
  return `${columnIndexToLetter(colIndex)}${rowIndex + 1}`
}

export const parseCellRef = (cellRef: string): { row: number; col: number } | null => {
  const match = cellRef.match(/^([A-Z]+)(\d+)$/)
  if (!match) return null

  let col = 0
  const letters = match[1]
  for (let i = 0; i < letters.length; i++) {
    col = col * 26 + (letters.charCodeAt(i) - 64)
  }
  col -= 1

  const row = parseInt(match[2], 10) - 1

  return { row, col }
}
