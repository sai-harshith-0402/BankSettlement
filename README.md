# Transaction-Settlement

TransactionSettlement Class Structure UML 
![bank_settlement_uml_v6_exceptions](https://github.com/user-attachments/assets/9ba042be-8fcb-4357-a68a-8ad18cad1297)

Database Structure UML
[bank_settlement_erd.html](https://github.com/user-attachments/files/26155850/bank_settlement_erd.html)

<style>
#erd svg { width: 100%; }
#erd svg.erDiagram .divider path { stroke-opacity: 0.4; }
#erd svg.erDiagram .row-rect-odd path,
#erd svg.erDiagram .row-rect-odd rect,
#erd svg.erDiagram .row-rect-even path,
#erd svg.erDiagram .row-rect-even rect { stroke: none !important; }
</style>
<div id="erd" style="padding: 8px 0;"></div>
<script type="module">
import mermaid from 'https://esm.sh/mermaid@11/dist/mermaid.esm.min.mjs';
const dark = matchMedia('(prefers-color-scheme: dark)').matches;
await document.fonts.ready;
mermaid.initialize({
  startOnLoad: false,
  theme: 'base',
  fontFamily: '"Anthropic Sans", sans-serif',
  themeVariables: {
    darkMode: dark,
    fontSize: '13px',
    fontFamily: '"Anthropic Sans", sans-serif',
    lineColor: dark ? '#9c9a92' : '#73726c',
    textColor: dark ? '#c2c0b6' : '#3d3d3a',
    primaryColor: dark ? '#2a2850' : '#EEEDFE',
    primaryBorderColor: dark ? '#534AB7' : '#AFA9EC',
    primaryTextColor: dark ? '#c2c0b6' : '#26215C',
    attributeBackgroundColorOdd: dark ? '#1e1e2e' : '#f8f7ff',
    attributeBackgroundColorEven: dark ? '#252535' : '#EEEDFE',
  },
});
const diagram = `erDiagram
  SETTLEMENT ||--o{ BATCH : contains
  BATCH ||--o{ TRANSACTION : groups

  SETTLEMENT {
    VARCHAR settlement_id PK
    DATE settlement_date
    VARCHAR status
  }

  BATCH {
    VARCHAR batch_id PK
    VARCHAR settlement_id FK
    DATE batch_date
    DECIMAL total_amount
  }

  TRANSACTION {
    VARCHAR transaction_id PK
    VARCHAR batch_id FK
    DECIMAL amount
    VARCHAR transaction_type
    DATE transaction_date
    VARCHAR transaction_state
    VARCHAR transaction_channel
  }
`;
const { svg } = await mermaid.render('erd-svg', diagram);
document.getElementById('erd').innerHTML = svg;

document.querySelectorAll('#erd svg.erDiagram .node').forEach(node => {
  const firstPath = node.querySelector('path[d]');
  if (!firstPath) return;
  const d = firstPath.getAttribute('d');
  const nums = d.match(/-?[\d.]+/g)?.map(Number);
  if (!nums || nums.length < 8) return;
  const xs = [nums[0], nums[2], nums[4], nums[6]];
  const ys = [nums[1], nums[3], nums[5], nums[7]];
  const x = Math.min(...xs), y = Math.min(...ys);
  const w = Math.max(...xs) - x, h = Math.max(...ys) - y;
  const rect = document.createElementNS('http://www.w3.org/2000/svg', 'rect');
  rect.setAttribute('x', x); rect.setAttribute('y', y);
  rect.setAttribute('width', w); rect.setAttribute('height', h);
  rect.setAttribute('rx', '8');
  for (const a of ['fill', 'stroke', 'stroke-width', 'class', 'style']) {
    if (firstPath.hasAttribute(a)) rect.setAttribute(a, firstPath.getAttribute(a));
  }
  firstPath.replaceWith(rect);
});

document.querySelectorAll('#erd svg.erDiagram .row-rect-odd path, #erd svg.erDiagram .row-rect-even path').forEach(p => {
  p.setAttribute('stroke', 'none');
});
</script>
