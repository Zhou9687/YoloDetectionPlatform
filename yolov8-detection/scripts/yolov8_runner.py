#!/usr/bin/env python3
import argparse
import json
import sys
from pathlib import Path


ROOT_DIR = Path(__file__).resolve().parent
LOCAL_ULTRA = ROOT_DIR / "ultralytics-main"
if LOCAL_ULTRA.exists():
    sys.path.insert(0, str(LOCAL_ULTRA))

from ultralytics import YOLO  # noqa: E402


def run_detect(args):
    model = YOLO(args.model)
    results = model.predict(
        source=args.image,
        conf=args.conf,
        iou=args.iou,
        device=args.device,
        imgsz=args.imgsz,
        augment=args.augment,
        max_det=args.max_det,
        verbose=False,
        save=False,
    )

    output = []
    if results:
        r = results[0]
        names = r.names if hasattr(r, "names") else {}
        boxes = r.boxes
        if boxes is not None:
            for i in range(len(boxes)):
                cls_id = int(boxes.cls[i].item())
                label = names.get(cls_id, str(cls_id)) if isinstance(names, dict) else str(cls_id)
                x, y, w, h = boxes.xywhn[i].tolist()
                conf = float(boxes.conf[i].item())
                output.append(
                    {
                        "label": label,
                        "x": float(x),
                        "y": float(y),
                        "width": float(w),
                        "height": float(h),
                        "confidence": conf,
                    }
                )

    print(json.dumps(output, ensure_ascii=True))


def run_train(args):
    dataset_arg = Path(args.dataset)
    if dataset_arg.suffix in [".yaml", ".yml"]:
        dataset_yaml = dataset_arg
    else:
        default_yaml = dataset_arg / "data.yaml"
        fallback_yaml = dataset_arg / "dataset.yaml"
        dataset_yaml = default_yaml if default_yaml.exists() else fallback_yaml
    if not dataset_yaml.exists():
        raise FileNotFoundError(f"dataset yaml not found: {dataset_yaml}")

    model = YOLO(args.model)
    last_progress = {"value": 0}

    def emit_progress(current_epoch, total_epochs):
        total = int(total_epochs or args.epochs or 0)
        current = int(current_epoch or 0) + 1
        if total <= 0:
            return
        progress = int((current * 100) / total)
        progress = max(1, min(99, progress))
        if progress <= last_progress["value"]:
            return
        last_progress["value"] = progress
        print(f"__PROGRESS__:{progress}", flush=True)

    def on_epoch_end(trainer):
        emit_progress(getattr(trainer, "epoch", 0), getattr(trainer, "epochs", args.epochs))

    # 不同 ultralytics 版本的事件名可能不同，这里两者都注册并由去重逻辑保证不重复刷进度。
    try:
        model.add_callback("on_train_epoch_end", on_epoch_end)
    except Exception:
        pass
    try:
        model.add_callback("on_fit_epoch_end", on_epoch_end)
    except Exception:
        pass

    model.train(
        data=str(dataset_yaml),
        epochs=args.epochs,
        imgsz=args.imgsz,
        batch=args.batch,
        conf=args.conf,
        device=args.device,
        project=args.project,
        name=args.name,
        exist_ok=True,
        verbose=False,
    )

    save_dir = Path(model.trainer.save_dir) if hasattr(model, "trainer") else None
    best_pt = str((save_dir / "weights" / "best.pt").resolve()) if save_dir else ""
    last_pt = str((save_dir / "weights" / "last.pt").resolve()) if save_dir else ""

    result = {
        "status": "ok",
        "epochs": args.epochs,
        "conf": args.conf,
        "dataset": str(dataset_yaml),
        "bestModelPath": best_pt,
        "lastModelPath": last_pt,
    }
    print(json.dumps(result, ensure_ascii=True), flush=True)


def build_parser():
    parser = argparse.ArgumentParser(description="YOLOv8 bridge script for Spring Boot")
    sub = parser.add_subparsers(dest="cmd", required=True)

    detect = sub.add_parser("detect")
    detect.add_argument("--image", required=True)
    detect.add_argument("--model", default="yolov8n.pt")
    detect.add_argument("--conf", type=float, default=0.25)
    detect.add_argument("--iou", type=float, default=0.45)
    detect.add_argument("--device", default="cpu")
    detect.add_argument("--imgsz", type=int, default=960)
    detect.add_argument("--augment", action="store_true")
    detect.add_argument("--max-det", dest="max_det", type=int, default=300)

    train = sub.add_parser("train")
    train.add_argument("--dataset", required=True)
    train.add_argument("--epochs", required=True, type=int)
    train.add_argument("--model", default="yolov8n.pt")
    train.add_argument("--imgsz", type=int, default=640)
    train.add_argument("--batch", type=int, default=16)
    train.add_argument("--conf", type=float, default=0.12)
    train.add_argument("--device", default="cpu")
    train.add_argument("--project", default="runs/train")
    train.add_argument("--name", default="exp")

    return parser


def main():
    args = build_parser().parse_args()
    try:
        if args.cmd == "detect":
            run_detect(args)
        else:
            run_train(args)
    except Exception as exc:
        print(str(exc), file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()

