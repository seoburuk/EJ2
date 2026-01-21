import html2canvas from 'html2canvas';

export const exportTimetableAsImage = async () => {
  const element = document.querySelector('.timetable-grid');
  if (!element) return;

  try {
    const canvas = await html2canvas(element as HTMLElement, {
      scale: 2, // 高画質
      backgroundColor: '#ffffff'
    });

    // 画像ダウンロード
    const link = document.createElement('a');
    link.download = `時間割_${new Date().getTime()}.png`;
    link.href = canvas.toDataURL('image/png');
    link.click();
  } catch (error) {
    console.error('エクスポートエラー', error);
  }
};