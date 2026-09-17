import { useState, useEffect, useRef } from "react";
import { 
  Layers, 
  Play, 
  Square, 
  CheckCircle2, 
  HelpCircle, 
  Info, 
  Maximize2, 
  Smartphone, 
  Crosshair, 
  Flame, 
  Timer, 
  TrendingUp, 
  Sliders, 
  Sparkles,
  Volume2,
  VolumeX,
  RotateCcw
} from "lucide-react";

export default function App() {
  const [hasPermission, setHasPermission] = useState(false);
  const [isServiceRunning, setIsServiceRunning] = useState(false);
  const [isExpanded, setIsExpanded] = useState(false);
  
  // Stats
  const [hits, setHits] = useState(0);
  const [headshots, setHeadshots] = useState(0);
  
  // Timer
  const [time, setTime] = useState(0);
  const [timerActive, setTimerActive] = useState(false);
  
  // Customizations
  const [themeIndex, setThemeIndex] = useState(0);
  const [opacity, setOpacity] = useState(0.95);
  const [haptic, setHaptic] = useState(true);
  
  // Checklist drills
  const [drills, setDrills] = useState([
    { id: 1, label: "Crosshair Placement (Head Level)", checked: false },
    { id: 2, label: "Recoil Management Training", checked: false },
    { id: 3, label: "Quick Gloo Wall Cover Practice", checked: false },
    { id: 4, label: "Drag Shot Calibration Drill", checked: false }
  ]);

  // Floating bubble position
  const [bubblePos, setBubblePos] = useState({ x: 50, y: 150 });
  const containerRef = useRef<HTMLDivElement>(null);
  const isDraggingRef = useRef(false);
  const dragStartRef = useRef({ x: 0, y: 0 });
  const bubblePosStartRef = useRef({ x: 0, y: 0 });

  const colors = [
    "bg-red-500", // Fire Red
    "bg-cyan-500", // Cyan Pro
    "bg-amber-500", // Gold Elite
    "bg-emerald-500" // Cobra Green
  ];

  const textColors = [
    "text-red-500",
    "text-cyan-500",
    "text-amber-500",
    "text-emerald-500"
  ];

  const borderColors = [
    "border-red-500",
    "border-cyan-500",
    "border-amber-500",
    "border-emerald-500"
  ];

  // Active Timer Effect
  useEffect(() => {
    let interval: any;
    if (timerActive) {
      interval = setInterval(() => {
        setTime(prev => prev + 1);
      }, 1000);
    } else {
      clearInterval(interval);
    }
    return () => clearInterval(interval);
  }, [timerActive]);

  const formatTime = (totalSeconds: number) => {
    const mins = Math.floor(totalSeconds / 60);
    const secs = totalSeconds % 60;
    return `${mins.toString().padStart(2, "0")}:${secs.toString().padStart(2, "0")}`;
  };

  const playTickSound = () => {
    if (!haptic) return;
    try {
      const audioCtx = new (window.AudioContext || (window as any).webkitAudioContext)();
      const osc = audioCtx.createOscillator();
      const gainNode = audioCtx.createGain();
      
      osc.type = "sine";
      osc.frequency.setValueAtTime(1000, audioCtx.currentTime);
      gainNode.gain.setValueAtTime(0.05, audioCtx.currentTime);
      gainNode.gain.exponentialRampToValueAtTime(0.001, audioCtx.currentTime + 0.05);
      
      osc.connect(gainNode);
      gainNode.connect(audioCtx.destination);
      
      osc.start();
      osc.stop(audioCtx.currentTime + 0.05);
    } catch (e) {
      // AudioContext blocker safety
    }
  };

  const handleStartService = () => {
    playTickSound();
    if (!hasPermission) {
      alert("Please grant 'Display over other apps' permission first in the left configuration console!");
      return;
    }
    setIsServiceRunning(true);
  };

  const handleStopService = () => {
    playTickSound();
    setIsServiceRunning(false);
    setIsExpanded(false);
  };

  // Drag and drop handlers
  const handleMouseDown = (e: React.MouseEvent) => {
    e.preventDefault();
    isDraggingRef.current = true;
    dragStartRef.current = { x: e.clientX, y: e.clientY };
    bubblePosStartRef.current = { ...bubblePos };
  };

  const handleMouseMove = (e: MouseEvent) => {
    if (!isDraggingRef.current || !containerRef.current) return;
    const dx = e.clientX - dragStartRef.current.x;
    const dy = e.clientY - dragStartRef.current.y;
    
    const containerRect = containerRef.current.getBoundingClientRect();
    const bubbleSize = isExpanded ? 300 : 56;
    
    let newX = bubblePosStartRef.current.x + dx;
    let newY = bubblePosStartRef.current.y + dy;
    
    // Bounds check
    newX = Math.max(0, Math.min(newX, containerRect.width - bubbleSize));
    newY = Math.max(0, Math.min(newY, containerRect.height - bubbleSize));
    
    setBubblePos({ x: newX, y: newY });
  };

  const handleMouseUp = () => {
    isDraggingRef.current = false;
  };

  useEffect(() => {
    window.addEventListener("mousemove", handleMouseMove);
    window.addEventListener("mouseup", handleMouseUp);
    return () => {
      window.removeEventListener("mousemove", handleMouseMove);
      window.removeEventListener("mouseup", handleMouseUp);
    };
  }, [bubblePos, isExpanded]);

  // Handle Touch Devices
  const handleTouchStart = (e: React.TouchEvent) => {
    isDraggingRef.current = true;
    const touch = e.touches[0];
    dragStartRef.current = { x: touch.clientX, y: touch.clientY };
    bubblePosStartRef.current = { ...bubblePos };
  };

  const handleTouchMove = (e: React.TouchEvent) => {
    if (!isDraggingRef.current || !containerRef.current) return;
    const touch = e.touches[0];
    const dx = touch.clientX - dragStartRef.current.x;
    const dy = touch.clientY - dragStartRef.current.y;
    
    const containerRect = containerRef.current.getBoundingClientRect();
    const bubbleSize = isExpanded ? 300 : 56;
    
    let newX = bubblePosStartRef.current.x + dx;
    let newY = bubblePosStartRef.current.y + dy;
    
    newX = Math.max(0, Math.min(newX, containerRect.width - bubbleSize));
    newY = Math.max(0, Math.min(newY, containerRect.height - bubbleSize));
    
    setBubblePos({ x: newX, y: newY });
  };

  // Simulated target ring click
  const [targets, setTargets] = useState<{ id: number; x: number; y: number; isHead: boolean }[]>([]);
  
  const spawnTarget = () => {
    const isHead = Math.random() > 0.65;
    const newTarget = {
      id: Date.now(),
      x: 10 + Math.random() * 80,
      y: 15 + Math.random() * 70,
      isHead
    };
    setTargets([newTarget]);
  };

  useEffect(() => {
    if (isServiceRunning) {
      spawnTarget();
    } else {
      setTargets([]);
    }
  }, [isServiceRunning]);

  const handleTargetClick = (target: any) => {
    playTickSound();
    if (target.isHead) {
      setHeadshots(prev => prev + 1);
      setHits(prev => prev + 1);
    } else {
      setHits(prev => prev + 1);
    }
    // Spawn next
    spawnTarget();
  };

  return (
    <div className="min-h-screen bg-zinc-950 text-zinc-100 flex flex-col md:flex-row font-sans">
      
      {/* LEFT: MainActivity Configuration Console */}
      <div className="w-full md:w-1/2 p-6 md:p-8 flex flex-col justify-between border-b md:border-b-0 md:border-r border-zinc-800 bg-zinc-900/60 backdrop-blur-md">
        <div className="space-y-6">
          {/* Header */}
          <div className="flex items-center gap-3">
            <div className="p-3 bg-orange-500/10 rounded-xl border border-orange-500/20 text-orange-400">
              <Layers size={28} />
            </div>
            <div>
              <h1 className="text-xl font-bold tracking-tight">FF Companion Console</h1>
              <p className="text-xs text-zinc-400">Native Android Service Configuration</p>
            </div>
          </div>

          {/* Legitimate Training Banner */}
          <div className="p-4 rounded-xl bg-zinc-800/80 border border-zinc-700/60 space-y-2">
            <div className="flex items-center gap-2 text-zinc-200 font-medium text-sm">
              <Info size={16} className="text-orange-400" />
              <span>Legitimate Training Helper</span>
            </div>
            <p className="text-xs leading-relaxed text-zinc-400">
              Your manual companion to track drills, timer duration, hits, and headshots. This application strictly contains <strong className="text-zinc-200">NO cheats, auto-aim, hacks, or automation</strong>.
            </p>
          </div>

          {/* SYSTEM_ALERT_WINDOW Permission Request */}
          <div className={`p-5 rounded-xl border transition-all ${
            hasPermission 
              ? "bg-emerald-500/5 border-emerald-500/20 text-emerald-400" 
              : "bg-amber-500/5 border-amber-500/20 text-amber-400"
          }`}>
            <div className="flex items-start gap-3">
              <div className="mt-1">
                <Smartphone size={20} />
              </div>
              <div className="space-y-1 flex-1">
                <h3 className="font-semibold text-sm">Overlay Draw Permission</h3>
                <p className="text-xs leading-relaxed text-zinc-400">
                  Required: <code className="bg-zinc-800 px-1 py-0.5 rounded text-zinc-300">android.permission.SYSTEM_ALERT_WINDOW</code>. This lets the companion widget show over your workspace when running.
                </p>
                <div className="pt-2">
                  <button 
                    onClick={() => {
                      playTickSound();
                      setHasPermission(true);
                    }}
                    disabled={hasPermission}
                    className={`text-xs font-semibold px-4 py-2 rounded-lg transition-all ${
                      hasPermission 
                        ? "bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 cursor-default" 
                        : "bg-amber-500 text-zinc-950 hover:bg-amber-400 hover:shadow-lg hover:shadow-amber-500/10 active:scale-95"
                    }`}
                  >
                    {hasPermission ? "✓ Permission Allowed" : "Grant SYSTEM_ALERT_WINDOW"}
                  </button>
                </div>
              </div>
            </div>
          </div>

          {/* Foreground Service Controls */}
          <div className="p-5 rounded-xl bg-zinc-900 border border-zinc-800 space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="font-semibold text-sm">OverlayService State</h3>
                <p className="text-xs text-zinc-400">Manage floating action widget runtime</p>
              </div>
              <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium ${
                isServiceRunning 
                  ? "bg-emerald-500/10 text-emerald-400 border border-emerald-500/20" 
                  : "bg-zinc-800 text-zinc-500 border border-zinc-700/50"
              }`}>
                <span className={`w-1.5 h-1.5 rounded-full ${isServiceRunning ? "bg-emerald-500 animate-pulse" : "bg-zinc-600"}`}></span>
                {isServiceRunning ? "Active" : "Inactive"}
              </span>
            </div>

            <div className="flex gap-3 pt-1">
              <button
                onClick={handleStartService}
                disabled={isServiceRunning}
                className="flex-1 flex items-center justify-center gap-2 text-xs font-bold px-4 py-3 rounded-lg bg-orange-500 text-zinc-950 hover:bg-orange-400 hover:shadow-lg hover:shadow-orange-500/10 active:scale-95 disabled:opacity-40 disabled:pointer-events-none transition-all"
              >
                <Play size={14} fill="currentColor" />
                Start Overlay Service
              </button>
              <button
                onClick={handleStopService}
                disabled={!isServiceRunning}
                className="flex-1 flex items-center justify-center gap-2 text-xs font-bold px-4 py-3 rounded-lg bg-zinc-800 text-zinc-300 border border-zinc-700 hover:bg-zinc-700 hover:text-zinc-100 active:scale-95 disabled:opacity-40 disabled:pointer-events-none transition-all"
              >
                <Square size={14} fill="currentColor" />
                Stop Service
              </button>
            </div>
          </div>

          {/* Feature highlights */}
          <div className="space-y-3">
            <h3 className="text-xs font-semibold uppercase tracking-wider text-zinc-500">Companion Features</h3>
            <div className="grid grid-cols-2 gap-3">
              <div className="p-3 bg-zinc-900/40 rounded-xl border border-zinc-800/80 space-y-1">
                <Timer size={16} className="text-orange-400" />
                <h4 className="text-xs font-semibold">Active Session Timer</h4>
                <p className="text-[10px] text-zinc-400">Start/Pause session timers seamlessly.</p>
              </div>
              <div className="p-3 bg-zinc-900/40 rounded-xl border border-zinc-800/80 space-y-1">
                <TrendingUp size={16} className="text-orange-400" />
                <h4 className="text-xs font-semibold">Manual Hits Log</h4>
                <p className="text-[10px] text-zinc-400">Increment and track headshots manually.</p>
              </div>
              <div className="p-3 bg-zinc-900/40 rounded-xl border border-zinc-800/80 space-y-1">
                <CheckCircle2 size={16} className="text-orange-400" />
                <h4 className="text-xs font-semibold">Structured Drills</h4>
                <p className="text-[10px] text-zinc-400">Custom checklist drills to improve aim.</p>
              </div>
              <div className="p-3 bg-zinc-900/40 rounded-xl border border-zinc-800/80 space-y-1">
                <Sliders size={16} className="text-orange-400" />
                <h4 className="text-xs font-semibold">Widget Styling</h4>
                <p className="text-[10px] text-zinc-400">Customize opacity, haptics, and colors.</p>
              </div>
            </div>
          </div>
        </div>

        {/* Build Integrity Notice */}
        <div className="pt-6 border-t border-zinc-800 flex items-center justify-between text-[11px] text-zinc-500">
          <span>Android Package: <code className="text-zinc-400">com.example</code></span>
          <span className="flex items-center gap-1"><Sparkles size={11} /> Default Debug Signed</span>
        </div>
      </div>

      {/* RIGHT: Active Target Training Simulator & Overlay Preview */}
      <div className="w-full md:w-1/2 p-6 md:p-8 flex flex-col items-center justify-center bg-zinc-950">
        <div className="w-full max-w-sm space-y-4">
          <div className="text-center space-y-1">
            <h2 className="text-sm font-semibold text-zinc-400 flex items-center justify-center gap-1.5">
              <Smartphone size={16} /> Device Training Ground
            </h2>
            <p className="text-xs text-zinc-500">Drag floating widget around; tap to expand it!</p>
          </div>

          {/* Smartphone Simulator Screen */}
          <div 
            ref={containerRef}
            className="relative w-full aspect-[9/16] max-h-[640px] rounded-[32px] border-[6px] border-zinc-800 bg-zinc-900 overflow-hidden shadow-2xl shadow-orange-500/5 select-none"
          >
            {/* Camera notch */}
            <div className="absolute top-3 left-1/2 -translate-x-1/2 w-20 h-4 bg-zinc-800 rounded-full z-20"></div>

            {/* Target Practice Arena */}
            <div className="absolute inset-0 bg-zinc-950 flex flex-col justify-between p-4 relative">
              {/* HUD Header */}
              <div className="flex items-center justify-between text-[10px] text-zinc-400 font-semibold pt-4">
                <div className="flex items-center gap-1 bg-zinc-900/60 px-2 py-1 rounded-full border border-zinc-800">
                  <Flame size={10} className="text-orange-500 animate-pulse" />
                  <span>FPS: 60</span>
                </div>
                <div className="flex items-center gap-1 bg-zinc-900/60 px-2 py-1 rounded-full border border-zinc-800">
                  <Crosshair size={10} />
                  <span>TRAINING GROUND</span>
                </div>
              </div>

              {/* Game Targets (if running) */}
              {isServiceRunning && targets.map(target => (
                <button
                  key={target.id}
                  onClick={() => handleTargetClick(target)}
                  style={{ left: `${target.x}%`, top: `${target.y}%` }}
                  className="absolute -translate-x-1/2 -translate-y-1/2 group cursor-crosshair focus:outline-none"
                >
                  <div className={`relative flex items-center justify-center rounded-full animate-ping-once transition-all ${
                    target.isHead ? "w-10 h-10 border-2 border-red-500 bg-red-500/10" : "w-14 h-14 border-2 border-orange-500 bg-orange-500/10"
                  }`}>
                    <div className={`w-3 h-3 rounded-full ${target.isHead ? "bg-red-500" : "bg-orange-500"}`}></div>
                    {/* Floating label */}
                    <span className="absolute -top-6 left-1/2 -translate-x-1/2 text-[9px] font-bold uppercase tracking-wider bg-zinc-900 px-1.5 py-0.5 rounded border border-zinc-800 text-zinc-300">
                      {target.isHead ? "Head" : "Body"}
                    </span>
                  </div>
                </button>
              ))}

              {/* Training Ground Bottom Banner */}
              {!isServiceRunning && (
                <div className="absolute inset-0 flex flex-col items-center justify-center p-6 text-center space-y-3 bg-zinc-900/40">
                  <div className="p-4 bg-zinc-800/80 rounded-2xl border border-zinc-700/60 max-w-xs space-y-2">
                    <Crosshair size={32} className="text-zinc-500 mx-auto" />
                    <h4 className="text-xs font-semibold text-zinc-300">Start Overlay Service First</h4>
                    <p className="text-[10px] text-zinc-500">
                      Enable display permissions and tap "Start Overlay Service" on the left menu to spawn the widget and targets!
                    </p>
                  </div>
                </div>
              )}

              {/* HUD Footer status */}
              <div className="text-[9px] text-zinc-500 text-center pb-2">
                Simulated HUD. Tapping targets simulates headshots & body hits.
              </div>
            </div>

            {/* FLOATING COMPANION OVERLAY WIDGET (Representing OverlayService.kt) */}
            {isServiceRunning && (
              <div
                style={{ 
                  left: `${bubblePos.x}px`, 
                  top: `${bubblePos.y}px`,
                  opacity: opacity
                }}
                className="absolute z-50 transition-shadow duration-150 select-none"
              >
                {!isExpanded ? (
                  /* COLLAPSED FLOATING BUBBLE */
                  <div
                    onMouseDown={handleMouseDown}
                    onTouchStart={handleTouchStart}
                    onClick={() => {
                      playTickSound();
                      setIsExpanded(true);
                    }}
                    className={`w-14 h-14 rounded-full flex items-center justify-center text-white shadow-xl cursor-grab active:cursor-grabbing hover:scale-105 transition-transform ${colors[themeIndex]}`}
                  >
                    <Layers size={24} className="animate-pulse" />
                  </div>
                ) : (
                  /* EXPANDED CONTROL PANEL */
                  <div
                    onMouseDown={handleMouseDown}
                    onTouchStart={handleTouchStart}
                    className="w-[280px] bg-zinc-900 border border-zinc-800 rounded-2xl shadow-2xl p-4 cursor-default select-none"
                    onClick={(e) => e.stopPropagation()}
                  >
                    {/* Header */}
                    <div className="flex items-center justify-between cursor-grab active:cursor-grabbing pb-2 border-b border-zinc-800">
                      <div className="flex items-center gap-1.5 text-xs font-bold text-zinc-200">
                        <Maximize2 size={12} className={textColors[themeIndex]} />
                        <span>FF Training Hub</span>
                      </div>
                      <button 
                        onClick={() => {
                          playTickSound();
                          setIsExpanded(false);
                        }}
                        className="text-zinc-400 hover:text-zinc-200 p-0.5 rounded hover:bg-zinc-800 transition-colors"
                      >
                        <RotateCcw size={14} />
                      </button>
                    </div>

                    {/* Timer */}
                    <div className="my-3 bg-zinc-950 p-2.5 rounded-lg flex items-center justify-between border border-zinc-800/80">
                      <div>
                        <span className="text-[10px] text-zinc-500 block uppercase font-semibold">Session Timer</span>
                        <span className={`text-base font-black tracking-tight ${textColors[themeIndex]}`}>{formatTime(time)}</span>
                      </div>
                      <div className="flex gap-1">
                        <button 
                          onClick={() => {
                            playTickSound();
                            setTimerActive(!timerActive);
                          }}
                          className={`p-1.5 rounded-md hover:bg-zinc-800 ${textColors[themeIndex]} transition-colors`}
                        >
                          <Play size={14} fill={timerActive ? "currentColor" : "none"} />
                        </button>
                        <button 
                          onClick={() => {
                            playTickSound();
                            setTimerActive(false);
                            setTime(0);
                          }}
                          className="p-1.5 rounded-md hover:bg-zinc-800 text-zinc-500 hover:text-zinc-300 transition-colors"
                        >
                          <RotateCcw size={14} />
                        </button>
                      </div>
                    </div>

                    {/* Stats Counter */}
                    <div className="grid grid-cols-2 gap-2.5 my-3">
                      <div className="bg-zinc-950 p-2 rounded-lg border border-zinc-800/80 text-center">
                        <span className="text-[9px] text-zinc-500 block">Total Hits</span>
                        <span className="text-base font-bold text-zinc-200">{hits}</span>
                        <div className="flex justify-center gap-2 mt-1">
                          <button 
                            onClick={() => {
                              playTickSound();
                              if (hits > 0) setHits(prev => prev - 1);
                            }}
                            className="bg-zinc-800 hover:bg-zinc-700 text-zinc-300 text-[10px] px-1.5 py-0.5 rounded active:scale-95 transition-all"
                          >
                            -
                          </button>
                          <button 
                            onClick={() => {
                              playTickSound();
                              setHits(prev => prev + 1);
                            }}
                            className={`hover:bg-opacity-20 text-xs px-1.5 py-0.5 rounded active:scale-95 transition-all bg-zinc-800 ${textColors[themeIndex]}`}
                          >
                            +
                          </button>
                        </div>
                      </div>

                      <div className="bg-zinc-950 p-2 rounded-lg border border-zinc-800/80 text-center">
                        <span className="text-[9px] text-zinc-500 block">Headshots</span>
                        <span className="text-base font-bold text-red-500">{headshots}</span>
                        <div className="flex justify-center gap-2 mt-1">
                          <button 
                            onClick={() => {
                              playTickSound();
                              if (headshots > 0) setHeadshots(prev => prev - 1);
                            }}
                            className="bg-zinc-800 hover:bg-zinc-700 text-zinc-300 text-[10px] px-1.5 py-0.5 rounded active:scale-95 transition-all"
                          >
                            -
                          </button>
                          <button 
                            onClick={() => {
                              playTickSound();
                              setHeadshots(prev => prev + 1);
                            }}
                            className="bg-zinc-800 hover:bg-zinc-700 text-red-500 text-xs px-1.5 py-0.5 rounded active:scale-95 transition-all"
                          >
                            +
                          </button>
                        </div>
                      </div>
                    </div>

                    {/* Drill tracker checklist */}
                    <div className="space-y-1.5 my-3">
                      <span className="text-[9px] text-zinc-500 block uppercase font-semibold">Drill Tracker</span>
                      {drills.map(drill => (
                        <label 
                          key={drill.id} 
                          className="flex items-center gap-2 text-[10px] text-zinc-300 hover:text-zinc-100 cursor-pointer"
                        >
                          <input 
                            type="checkbox" 
                            checked={drill.checked}
                            onChange={(e) => {
                              playTickSound();
                              setDrills(prev => prev.map(d => d.id === drill.id ? { ...d, checked: e.target.checked } : d));
                            }}
                            className="rounded border-zinc-700 text-orange-500 focus:ring-0 focus:ring-offset-0 bg-zinc-950 w-3 h-3 cursor-pointer"
                          />
                          <span className={drill.checked ? "line-through text-zinc-500" : ""}>{drill.label}</span>
                        </label>
                      ))}
                    </div>

                    {/* Styling Controls */}
                    <div className="pt-2 border-t border-zinc-800 space-y-2">
                      <div className="flex items-center justify-between">
                        <span className="text-[9px] text-zinc-500 font-semibold">THEME</span>
                        <div className="flex gap-1.5">
                          {colors.map((color, idx) => (
                            <button
                              key={idx}
                              onClick={() => {
                                playTickSound();
                                setThemeIndex(idx);
                              }}
                              className={`w-4.5 h-4.5 rounded-full ${color} border transition-all ${themeIndex === idx ? "border-white scale-110" : "border-transparent opacity-60"}`}
                            />
                          ))}
                        </div>
                      </div>

                      <div className="space-y-1">
                        <div className="flex items-center justify-between text-[9px] text-zinc-500">
                          <span>OPACITY</span>
                          <span>{Math.round(opacity * 100)}%</span>
                        </div>
                        <input 
                          type="range" 
                          min="0.4" 
                          max="1.0" 
                          step="0.05"
                          value={opacity}
                          onChange={(e) => setOpacity(parseFloat(e.target.value))}
                          className={`w-full accent-orange-500 h-1 bg-zinc-800 rounded-lg cursor-pointer appearance-none`}
                        />
                      </div>

                      {/* Footer toggle */}
                      <div className="flex items-center justify-between pt-1 text-[10px] text-zinc-400">
                        <button
                          onClick={() => setHaptic(!haptic)}
                          className="flex items-center gap-1 hover:text-zinc-200 transition-colors"
                        >
                          {haptic ? <Volume2 size={12} /> : <VolumeX size={12} />}
                          <span>{haptic ? "Haptics On" : "Muted"}</span>
                        </button>
                        <button
                          onClick={() => {
                            playTickSound();
                            setHits(0);
                            setHeadshots(0);
                            setTimerActive(false);
                            setTime(0);
                            setDrills(prev => prev.map(d => ({ ...d, checked: false })));
                          }}
                          className="hover:underline"
                        >
                          Reset All
                        </button>
                        <button
                          onClick={() => {
                            playTickSound();
                            setIsExpanded(false);
                          }}
                          className="font-bold text-zinc-300 hover:text-white"
                        >
                          Collapse
                        </button>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      </div>

    </div>
  );
}
